package com.ztech.crm.offerings.dto.response;

import com.ztech.crm.offerings.domain.enums.VenueStatus;
import java.math.BigDecimal;
import java.util.List;

public record VenueResponse(
        Long id,
        String name,
        Integer capacity,
        BigDecimal rate,
        String address,
        String locality,
        String description,
        List<String> equipment,
        VenueStatus status
) {
}
