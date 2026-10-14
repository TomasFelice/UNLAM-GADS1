package com.ztech.crm.customers.dto.response;

import com.ztech.crm.customers.domain.enums.PartyStatus;

/** Datos de empresa; contactos asociados se consultan paginados por companyId (BE-CUS-03). */
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
        String salesRepName,
        Long originId,
        String notes
) {
    public String name() {
        return businessName;
    }
}
