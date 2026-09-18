package com.ztech.crm.customers.dto.response;

import com.ztech.crm.customers.domain.enums.PartyStatus;

/** Resumen de un contacto, embebido en {@link CompanyDetailResponse} (BE-CUS-03). */
public record ContactSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        PartyStatus status,
        Long companyId,
        Long salesRepId
) {
}
