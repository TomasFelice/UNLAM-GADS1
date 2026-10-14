package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.ActivityType;
import com.ztech.crm.catalogs.dto.request.CatalogEntryRequest;
import com.ztech.crm.catalogs.dto.response.ActivityTypeResponse;
import com.ztech.crm.catalogs.repository.ActivityTypeRepository;
import com.ztech.crm.catalogs.repository.specification.CatalogSpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityTypeService {
    private final ActivityTypeRepository repository;
    public ActivityTypeService(ActivityTypeRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<ActivityTypeResponse> list(String q, Boolean active, Pageable pageable) {
        var filter = CatalogSpecifications.<ActivityType>filter(TenantContext.currentTenantId(), q, active);
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(repository.findAll(filter, stable), this::map);
    }
    @Transactional(readOnly = true)
    public ActivityTypeResponse getDetail(Long id) { return map(getOwnedOrThrow(id)); }

    @Transactional(readOnly = true)
    public CatalogEntryInfo getSummary(Long id) {
        var value = getOwnedOrThrow(id);
        return new CatalogEntryInfo(value.getId(), value.getName(), value.isActive());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ActivityTypeResponse create(CatalogEntryRequest request) {
        Long tenant = TenantContext.currentTenantId();
        if (repository.existsByTenantIdAndName(tenant, request.name())) duplicate();
        return map(repository.saveAndFlush(new ActivityType(tenant, request.name(), request.active())));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ActivityTypeResponse update(Long id, CatalogEntryRequest request) {
        var value = getOwnedOrThrow(id);
        if (repository.existsByTenantIdAndNameAndIdNot(TenantContext.currentTenantId(), request.name(), id)) duplicate();
        value.updateDetails(request.name(), request.active());
        return map(repository.saveAndFlush(value));
    }
    /** Conserva referencias anteriores; exige actividad en nuevas asociaciones. */
    @Transactional(readOnly = true)
    public void assertSelectable(Long id, Long previousId) {
        if (id == null) return;
        var value = getOwnedOrThrow(id);
        if (!value.isActive() && !id.equals(previousId)) {
            throw new BusinessException("ACTIVITY_TYPE_INACTIVE", "El tipo de actividad seleccionado no está activo.");
        }
    }
    private void duplicate() {
        throw new ConflictException("ACTIVITY_TYPE_NAME_DUPLICATE", "Ya existe un tipo de actividad con ese nombre en la organización.");
    }
    private ActivityType getOwnedOrThrow(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el tipo de actividad solicitado."));
    }
    private ActivityTypeResponse map(ActivityType value) {
        return new ActivityTypeResponse(value.getId(), value.getName(), value.isActive());
    }
}
