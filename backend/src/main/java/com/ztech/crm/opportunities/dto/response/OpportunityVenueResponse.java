package com.ztech.crm.opportunities.dto.response;


import java.math.BigDecimal;
import java.util.List;

public record OpportunityVenueResponse(
        Long id,
        String name,
        Integer capacity,
        BigDecimal rate,
        String address,
        String locality,
        String description,
        List<String> equipment,
        String status
) {
}
