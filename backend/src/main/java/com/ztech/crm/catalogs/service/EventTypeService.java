package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.EventType;
import com.ztech.crm.catalogs.dto.request.CatalogEntryRequest;
import com.ztech.crm.catalogs.dto.response.EventTypeResponse;
import com.ztech.crm.catalogs.repository.EventTypeRepository;
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
public class EventTypeService {
    private final EventTypeRepository repository;
    public EventTypeService(EventTypeRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<EventTypeResponse> list(String q, Boolean active, Pageable pageable) {
        var filter = CatalogSpecifications.<EventType>filter(TenantContext.currentTenantId(), q, active);
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(repository.findAll(filter, stable), this::map);
    }
    @Transactional(readOnly = true)
    public EventTypeResponse getDetail(Long id) { return map(getOwnedOrThrow(id)); }

    @Transactional(readOnly = true)
    public CatalogEntryInfo getSummary(Long id) {
        var value = getOwnedOrThrow(id);
        return new CatalogEntryInfo(value.getId(), value.getName(), value.isActive());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventTypeResponse create(CatalogEntryRequest request) {
        Long tenant = TenantContext.currentTenantId();
        if (repository.existsByTenantIdAndName(tenant, request.name())) duplicate();
        return map(repository.saveAndFlush(new EventType(tenant, request.name(), request.active())));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventTypeResponse update(Long id, CatalogEntryRequest request) {
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
            throw new BusinessException("EVENT_TYPE_INACTIVE", "El tipo de evento seleccionado no está activo.");
        }
    }
    private void duplicate() {
        throw new ConflictException("EVENT_TYPE_NAME_DUPLICATE", "Ya existe un tipo de evento con ese nombre en la organización.");
    }
    private EventType getOwnedOrThrow(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el tipo de evento solicitado."));
    }
    private EventTypeResponse map(EventType value) {
        return new EventTypeResponse(value.getId(), value.getName(), value.isActive());
    }
}
