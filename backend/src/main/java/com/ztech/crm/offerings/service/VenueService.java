package com.ztech.crm.offerings.service;

import com.ztech.crm.offerings.domain.Venue;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.offerings.mapper.VenueMapper;
import com.ztech.crm.offerings.repository.VenueRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BE-OFF-01. Sólo lectura en E1; el ABM llega en la Fase 6 (BE-OFF-02). */
@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueService(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> list() {
        Long tenantId = TenantContext.currentTenantId();
        return venueRepository.findAllByTenantIdOrderByName(tenantId).stream().map(venueMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse getDetail(Long id) {
        return venueMapper.toResponse(getOwnedOrThrow(id));
    }

    /**
     * Para uso cross-módulo (p. ej. {@code OpportunityService} al crear una
     * oportunidad — BE-OFF-05: un salón desactivado no puede seleccionarse en altas
     * nuevas, aunque conserve las oportunidades ya creadas con él).
     */
    @Transactional(readOnly = true)
    public VenueResponse getActiveOrThrow(Long id) {
        Venue venue = getOwnedOrThrow(id);
        if (!venue.isAvailable()) {
            throw new BusinessException("VENUE_UNAVAILABLE", "El salón seleccionado no está disponible.");
        }
        return venueMapper.toResponse(venue);
    }

    private Venue getOwnedOrThrow(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        return venueRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("No se encontró el salón solicitado."));
    }
}
