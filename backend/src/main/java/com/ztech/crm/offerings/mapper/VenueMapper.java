package com.ztech.crm.offerings.mapper;

import com.ztech.crm.offerings.domain.Venue;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    public VenueResponse toResponse(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getCapacity(), venue.getRate(),
                venue.getAddress(), venue.getLocality(), venue.getDescription(), venue.getEquipment(),
                venue.getStatus());
    }
}
