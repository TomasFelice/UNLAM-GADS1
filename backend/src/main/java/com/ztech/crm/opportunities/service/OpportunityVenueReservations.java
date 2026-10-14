package com.ztech.crm.opportunities.service;

import com.ztech.crm.offerings.service.VenueReservationPort;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityVenueReservations implements VenueReservationPort {
    private final OpportunityRepository repository;

    public OpportunityVenueReservations(OpportunityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReservationAboveCapacity(Long tenantId, Long venueId, int capacity) {
        return repository.hasWonReservationAboveCapacity(tenantId, venueId, capacity);
    }
}
