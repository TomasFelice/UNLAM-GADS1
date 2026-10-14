package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.Pipeline;
import com.ztech.crm.catalogs.domain.Stage;
import com.ztech.crm.catalogs.domain.enums.StageKind;
import com.ztech.crm.catalogs.dto.request.CatalogEntryRequest;
import com.ztech.crm.catalogs.dto.request.CreatePipelineRequest;
import com.ztech.crm.catalogs.dto.response.PipelineResponse;
import com.ztech.crm.catalogs.dto.response.PipelineTemplateResponse;
import com.ztech.crm.catalogs.dto.response.PipelineTemplateResponse.TemplateStage;
import com.ztech.crm.catalogs.repository.PipelineRepository;
import com.ztech.crm.catalogs.repository.StageRepository;
import com.ztech.crm.catalogs.repository.specification.CatalogSpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import com.ztech.crm.tenancy.service.TenantConfigurationService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineService {
    private final PipelineRepository repository;
    private final StageRepository stages;
    private final TenantConfigurationService configuration;

    public PipelineService(PipelineRepository repository, StageRepository stages, TenantConfigurationService configuration) {
        this.repository = repository; this.stages = stages; this.configuration = configuration;
    }

    @Transactional(readOnly = true)
    public PageResponse<PipelineResponse> list(String q, Boolean active, Pageable pageable) {
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(repository.findAll(CatalogSpecifications.filter(TenantContext.currentTenantId(), q, active), stable), this::map);
    }
    @Transactional(readOnly = true)
    public PipelineResponse getDetail(Long id) { return map(owned(id)); }
    @Transactional(readOnly = true)
    public PipelineInfo getInfo(Long id) { return info(owned(id)); }
    @Transactional(readOnly = true)
    public PipelineInfo getDefault() {
        return info(repository.findByTenantIdAndDefaultPipelineTrue(TenantContext.currentTenantId())
                .orElseThrow(() -> new ConflictException("PIPELINE_DEFAULT_MISSING", "La organización no tiene un embudo predeterminado.")));
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public PipelineInfo assertActive(Long id) {
        var pipeline = lockForUse(id);
        if (!pipeline.isActive()) throw new ConflictException("PIPELINE_INACTIVE", "El embudo seleccionado está inactivo.");
        return info(pipeline);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PipelineResponse create(CreatePipelineRequest request) {
        configuration.lockCurrent();
        Long tenant = TenantContext.currentTenantId();
        if (repository.existsByTenantIdAndName(tenant, request.name())) duplicate();
        List<TemplateStage> copied = request.templateKey() == null || request.templateKey().isBlank() ? List.of()
                : templates().stream().filter(t -> t.key().equals(request.templateKey())).findFirst()
                    .orElseThrow(() -> new NotFoundException("No se encontró la plantilla solicitada.")).stages();
        var pipeline = repository.saveAndFlush(new Pipeline(tenant, request.name()));
        for (int i = 0; i < copied.size(); i++) {
            var stage = copied.get(i);
            stages.save(new Stage(tenant, pipeline.getId(), stage.name(), i + 1, StageKind.valueOf(stage.kind())));
        }
        return map(pipeline);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PipelineResponse update(Long id, CatalogEntryRequest request) {
        configuration.lockCurrent();
        var pipeline = lockOwned(id);
        if (repository.existsByTenantIdAndNameAndIdNot(TenantContext.currentTenantId(), request.name(), id)) duplicate();
        if (!request.active() && pipeline.isDefaultPipeline()) {
            throw new ConflictException("PIPELINE_DEFAULT_REQUIRED", "Elegí otro embudo predeterminado antes de desactivar éste.");
        }
        if (request.active()) assertComplete(id);
        pipeline.updateDetails(request.name(), request.active());
        return map(repository.saveAndFlush(pipeline));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PipelineResponse makeDefault(Long id) {
        configuration.lockCurrent();
        var target = lockOwned(id);
        if (!target.isActive()) throw new ConflictException("PIPELINE_INACTIVE", "El embudo predeterminado debe estar activo.");
        assertComplete(id);
        var previous = repository.findByTenantIdAndDefaultPipelineTrue(TenantContext.currentTenantId()).orElse(null);
        if (previous != null && !previous.getId().equals(id)) {
            previous.setDefaultPipeline(false);
            repository.saveAndFlush(previous);
        }
        target.setDefaultPipeline(true);
        return map(repository.saveAndFlush(target));
    }

    Pipeline lockForUse(Long id) {
        return repository.lockReadable(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el embudo solicitado."));
    }
    Pipeline lockOwned(Long id) {
        return repository.lockOwned(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el embudo solicitado."));
    }
    void assertComplete(Long id) {
        for (var kind : StageKind.values()) {
            if (stages.countByTenantIdAndPipelineIdAndActiveTrueAndKind(TenantContext.currentTenantId(), id, kind) == 0) {
                throw new ConflictException("PIPELINE_INCOMPLETE", "El embudo activo necesita etapas activas abiertas, ganadas y perdidas. Falta: " + kind + ".");
            }
        }
    }
    @Transactional(readOnly = true)
    public List<PipelineTemplateResponse> templates() {
        var won = new TemplateStage("Ganada / Reservado", "WON");
        var lost = new TemplateStage("Perdida", "LOST");
        return List.of(new PipelineTemplateResponse("corporate-events", "Eventos corporativos", List.of(
                new TemplateStage("Consulta recibida", "OPEN"), new TemplateStage("Necesidad relevada", "OPEN"),
                new TemplateStage("Visita al salón", "OPEN"), new TemplateStage("Propuesta enviada", "OPEN"),
                new TemplateStage("Negociación", "OPEN"), won, lost)),
                new PipelineTemplateResponse("brief-hiring", "Contratación breve", List.of(
                        new TemplateStage("Consulta recibida", "OPEN"), new TemplateStage("Propuesta enviada", "OPEN"), won, lost)));
    }
    private Pipeline owned(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el embudo solicitado."));
    }
    private PipelineInfo info(Pipeline p) { return new PipelineInfo(p.getId(), p.getName(), p.isActive(), p.isDefaultPipeline()); }
    private PipelineResponse map(Pipeline p) { return new PipelineResponse(p.getId(), p.getName(), p.isActive(), p.isDefaultPipeline(), p.getCreatedAt(), p.getUpdatedAt()); }
    private void duplicate() { throw new ConflictException("PIPELINE_NAME_DUPLICATE", "Ya existe un embudo con ese nombre en la organización."); }
}
