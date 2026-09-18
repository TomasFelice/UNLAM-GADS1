package com.ztech.crm.customers.dto.response;

import com.ztech.crm.customers.domain.enums.PartyStatus;
import java.util.List;

/** {@code GET /api/v1/companies/{id}} incluye sus contactos (BE-CUS-03). */
public record CompanyDetailResponse(
        Long id,
        String legalName,
        String businessName,
        String cuit,
        String industry,
        String email,
        String phone,
        String address,
        String locality,
        String website,
        PartyStatus status,
        Long salesRepId,
        Long originId,
        String notes,
        List<ContactSummaryResponse> contacts
) {
    public String name() {
        return businessName;
    }
}
