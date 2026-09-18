package com.ztech.crm.offerings.service;

import com.ztech.crm.offerings.domain.EventService;
import com.ztech.crm.offerings.dto.response.EventServiceResponse;
import com.ztech.crm.offerings.mapper.EventServiceMapper;
import com.ztech.crm.offerings.repository.EventServiceRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventServiceService {

    private final EventServiceRepository eventServiceRepository;
    private final EventServiceMapper eventServiceMapper;

    public EventServiceService(EventServiceRepository eventServiceRepository, EventServiceMapper eventServiceMapper) {
        this.eventServiceRepository = eventServiceRepository;
        this.eventServiceMapper = eventServiceMapper;
    }

    @Transactional(readOnly = true)
    public List<EventServiceResponse> list() {
        return eventServiceRepository.findAllByTenantIdOrderByName(TenantContext.currentTenantId()).stream()
                .map(eventServiceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventServiceResponse getDetail(Long id) {
        return eventServiceMapper.toResponse(getOwnedOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EventServiceResponse getActiveOrThrow(Long id) {
        EventService eventService = getOwnedOrThrow(id);
        if (!eventService.isActive()) {
            throw new BusinessException("EVENT_SERVICE_INACTIVE", "El servicio seleccionado no está activo.");
        }
        return eventServiceMapper.toResponse(eventService);
    }

    private EventService getOwnedOrThrow(Long id) {
        return eventServiceRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el servicio solicitado."));
    }
}
