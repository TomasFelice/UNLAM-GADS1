package com.ztech.crm.opportunities.dto.response;

public record VenueAvailabilityResponse(boolean available, boolean venueAvailable,
        boolean capacitySufficient, boolean rangeFree) { }
