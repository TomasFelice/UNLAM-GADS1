package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.EventType;
import com.ztech.crm.catalogs.dto.response.EventTypeResponse;
import com.ztech.crm.catalogs.repository.EventTypeRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;

    public EventTypeService(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<EventTypeResponse> list() {
        return eventTypeRepository.findAllByTenantIdOrderByName(TenantContext.currentTenantId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventTypeResponse getActiveOrThrow(Long id) {
        EventType type = eventTypeRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el tipo de evento solicitado."));
        if (!type.isActive()) {
            throw new BusinessException("EVENT_TYPE_INACTIVE", "El tipo de evento seleccionado no está activo.");
        }
        return toResponse(type);
    }

    private EventTypeResponse toResponse(EventType type) {
        return new EventTypeResponse(type.getId(), type.getName(), type.isActive());
    }
}
