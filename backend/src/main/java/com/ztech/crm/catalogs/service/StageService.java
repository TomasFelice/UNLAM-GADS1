package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.Stage;
import com.ztech.crm.catalogs.domain.enums.StageKind;
import com.ztech.crm.catalogs.dto.request.StageRequest;
import com.ztech.crm.catalogs.dto.request.ReorderStagesRequest;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.catalogs.mapper.StageMapper;
import com.ztech.crm.catalogs.repository.StageRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import com.ztech.crm.tenancy.service.TenantConfigurationService;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageService {
    private final StageRepository repository;
    private final StageMapper mapper;
    private final PipelineService pipelines;
    private final TenantConfigurationService configuration;
    private final StageUsagePort usage;
    private final EntityManager entityManager;
    public StageService(StageRepository repository, StageMapper mapper, PipelineService pipelines,
                        TenantConfigurationService configuration, StageUsagePort usage, EntityManager entityManager) {
        this.repository = repository; this.mapper = mapper; this.pipelines = pipelines;
        this.configuration = configuration; this.usage = usage; this.entityManager = entityManager;
    }
    @Transactional(readOnly = true)
    public List<StageResponse> list(Long pipelineId) { return ownedStages(resolvePipeline(pipelineId)).stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<StageInfo> listInfo(Long pipelineId) { return ownedStages(resolvePipeline(pipelineId)).stream().map(this::info).toList(); }
    @Transactional(readOnly = true)
    public StageResponse getDetail(Long id) { return mapper.toResponse(owned(id)); }
    @Transactional(readOnly = true)
    public StageInfo getSummary(Long id) { return info(owned(id)); }

    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getNames(java.util.Set<Long> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        return repository.findAllByTenantIdAndIdIn(TenantContext.currentTenantId(), ids).stream()
                .collect(java.util.stream.Collectors.toMap(Stage::getId, Stage::getName));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public StageInfo assertOpenAndActive(Long id, Long pipelineId) {
        pipelines.lockForUse(pipelineId);
        var stage = repository.lockReadable(id, TenantContext.currentTenantId()).orElseThrow(() -> new NotFoundException("No se encontró la etapa solicitada."));
        if (!stage.getPipelineId().equals(pipelineId)) throw new ConflictException("STAGE_PIPELINE_MISMATCH", "La etapa pertenece a otro embudo.");
        if (!stage.isActive()) throw new BusinessException("STAGE_INACTIVE", "La etapa seleccionada está inactiva.");
        if (stage.getKind() != StageKind.OPEN) throw new BusinessException("STAGE_NOT_OPEN", "La oportunidad debe crearse en una etapa abierta.");
        return info(stage);
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public StageInfo assertKindAndActive(Long id, Long pipelineId, String kind) {
        pipelines.lockForUse(pipelineId);
        var stage = repository.lockReadable(id, TenantContext.currentTenantId()).orElseThrow(() -> new NotFoundException("No se encontró la etapa solicitada."));
        if (!stage.getPipelineId().equals(pipelineId)) throw new ConflictException("STAGE_PIPELINE_MISMATCH", "La etapa pertenece a otro embudo.");
        if (!stage.isActive()) throw new ConflictException("STAGE_INACTIVE", "La etapa seleccionada está inactiva.");
        if (!stage.getKind().name().equals(kind)) throw new ConflictException("STAGE_KIND_MISMATCH", "La etapa no corresponde a esta operación.");
        return info(stage);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StageResponse create(StageRequest request) {
        configuration.lockCurrent();
        var pipeline = pipelines.lockOwned(request.pipelineId());
        int position = ownedStages(pipeline.getId()).stream().mapToInt(Stage::getPosition).max().orElse(0) + 1;
        var stage = new Stage(TenantContext.currentTenantId(), pipeline.getId(), request.name(), position, request.kind());
        stage.updateDetails(request.name(), request.kind(), request.active());
        return mapper.toResponse(repository.saveAndFlush(stage));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StageResponse update(Long id, StageRequest request) {
        configuration.lockCurrent();
        var stage = owned(id);
        var pipeline = pipelines.lockOwned(stage.getPipelineId());
        stage = repository.lockOwned(id, TenantContext.currentTenantId()).orElseThrow(() -> new NotFoundException("No se encontró la etapa solicitada."));
        if (!request.pipelineId().equals(stage.getPipelineId())) throw new ConflictException("STAGE_PIPELINE_IMMUTABLE", "No se puede mover una etapa a otro embudo.");
        if (request.kind() != stage.getKind() && usage.isUsed(TenantContext.currentTenantId(), id)) {
            throw new ConflictException("STAGE_KIND_IMMUTABLE", "No se puede cambiar el tipo de una etapa con historia comercial.");
        }
        stage.updateDetails(request.name(), request.kind(), request.active());
        repository.saveAndFlush(stage);
        if (pipeline.isActive()) pipelines.assertComplete(pipeline.getId());
        return mapper.toResponse(stage);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<StageResponse> reorder(Long pipelineId, ReorderStagesRequest request) {
        configuration.lockCurrent();
        pipelines.lockOwned(pipelineId);
        var current = ownedStages(pipelineId);
        Map<Long, Stage> byId = current.stream().collect(Collectors.toMap(Stage::getId, s -> s));
        var ids = request.stageIds();
        if (ids.size() != byId.size() || new HashSet<>(ids).size() != ids.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BusinessException("INVALID_STAGE_ORDER", "Enviá todas las etapas del embudo, una vez cada una.");
        }
        entityManager.createNativeQuery("SET CONSTRAINTS uq_stages_pipeline_position DEFERRED").executeUpdate();
        for (int i = 0; i < ids.size(); i++) byId.get(ids.get(i)).reorder(i + 1);
        repository.flush();
        return ids.stream().map(id -> mapper.toResponse(byId.get(id))).toList();
    }
    private Long resolvePipeline(Long id) { return id == null ? pipelines.getDefault().id() : pipelines.getInfo(id).id(); }
    private List<Stage> ownedStages(Long id) { return repository.findAllByTenantIdAndPipelineIdOrderByPosition(TenantContext.currentTenantId(), id); }
    private Stage owned(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.currentTenantId()).orElseThrow(() -> new NotFoundException("No se encontró la etapa solicitada."));
    }
    private StageInfo info(Stage s) { return new StageInfo(s.getId(), s.getPipelineId(), s.getName(), s.getPosition(), s.getKind().name(), s.isActive()); }
}
