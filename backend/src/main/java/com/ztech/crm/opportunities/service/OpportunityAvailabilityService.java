package com.ztech.crm.opportunities.service;

import com.ztech.crm.offerings.service.VenueService;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.dto.response.VenueAvailabilityResponse;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class OpportunityAvailabilityService {
    private final OpportunityRepository repository;
    private final VenueService venues;
    private final OpportunityAccessService access;
    public OpportunityAvailabilityService(OpportunityRepository repository, VenueService venues, OpportunityAccessService access) {
        this.repository = repository;
        this.venues = venues;
        this.access = access;
    }
    @Transactional(readOnly = true)
    public VenueAvailabilityResponse get(Long id) {
        var opportunity = access.getReadableOrThrow(id);
        var venue = venues.getInfo(opportunity.getVenueId());
        boolean venueAvailable = "DISPONIBLE".equals(venue.status());
        boolean capacity = opportunity.getAttendeeCount() <= venue.capacity();
        boolean free = !overlaps(opportunity);
        return new VenueAvailabilityResponse(venueAvailable && capacity && free, venueAvailable, capacity, free);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertConfirmable(Opportunity opportunity) {
        var venue = venues.getForConfirmation(opportunity.getVenueId());
        if (!"DISPONIBLE".equals(venue.status()))
            throw new BusinessException("VENUE_UNAVAILABLE", "El salón seleccionado no está disponible.");
        if (opportunity.getAttendeeCount() > venue.capacity())
            throw new BusinessException("VENUE_CAPACITY_EXCEEDED", "La cantidad de asistentes supera la capacidad del salón.");
        if (overlaps(opportunity))
            throw new ConflictException("VENUE_ALREADY_BOOKED", "El salón ya tiene una reserva en ese horario. Elegí otro horario o salón.");
    }
    private boolean overlaps(Opportunity opportunity) {
        return repository.hasOverlappingReservation(TenantContext.currentTenantId(), opportunity.getVenueId(),
                opportunity.getId(), opportunity.getEventStart(), opportunity.getEventEnd());
    }
}
