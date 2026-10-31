package com.ztech.crm.offerings.service;

import com.ztech.crm.offerings.domain.EventService;
import com.ztech.crm.offerings.dto.response.EventServiceResponse;
import com.ztech.crm.offerings.dto.request.EventServiceRequest;
import com.ztech.crm.offerings.mapper.EventServiceMapper;
import com.ztech.crm.offerings.repository.EventServiceRepository;
import com.ztech.crm.offerings.repository.specification.EventServiceSpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventServiceService {

    private final EventServiceRepository eventServiceRepository;
    private final EventServiceMapper eventServiceMapper;

    public EventServiceService(EventServiceRepository eventServiceRepository, EventServiceMapper eventServiceMapper) {
        this.eventServiceRepository = eventServiceRepository;
        this.eventServiceMapper = eventServiceMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventServiceResponse create(EventServiceRequest request) {
        EventService service = new EventService(TenantContext.currentTenantId(), request.name());
        apply(service, request);
        return eventServiceMapper.toResponse(eventServiceRepository.save(service));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventServiceResponse update(Long id, EventServiceRequest request) {
        EventService service = getOwnedOrThrow(id);
        apply(service, request);
        return eventServiceMapper.toResponse(eventServiceRepository.save(service));
    }

    private void apply(EventService service, EventServiceRequest request) {
        service.updateDetails(request.name(), request.description(), request.price(), request.active());
    }

    @Transactional(readOnly = true)
    public PageResponse<EventServiceResponse> list(String q, Boolean active, Pageable pageable) {
        var filters = EventServiceSpecifications.tenant(TenantContext.currentTenantId());
        if (q != null && !q.isBlank()) filters = filters.and(EventServiceSpecifications.contains(q));
        if (active != null) filters = filters.and(EventServiceSpecifications.active(active));
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(eventServiceRepository.findAll(filters, stable), eventServiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventServiceResponse getDetail(Long id) {
        return eventServiceMapper.toResponse(getOwnedOrThrow(id));
    }

    @Transactional(readOnly = true)
    public void assertSelectable(Long id, boolean previouslyAssociated) {
        EventService eventService = getOwnedOrThrow(id);
        if (!previouslyAssociated && !eventService.isActive()) {
            throw new BusinessException("EVENT_SERVICE_INACTIVE", "El servicio seleccionado no está activo.");
        }
    }

    private EventService getOwnedOrThrow(Long id) {
        return eventServiceRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el servicio solicitado."));
    }

    @Transactional(readOnly = true)
    public EventServiceInfo getInfo(Long id) {
        var value = getOwnedOrThrow(id);
        return new EventServiceInfo(value.getId(), value.getName(), value.getPrice(), value.isActive());
    }
}
