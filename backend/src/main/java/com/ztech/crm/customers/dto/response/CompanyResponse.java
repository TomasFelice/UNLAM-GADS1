package com.ztech.crm.customers.dto.response;

import com.ztech.crm.customers.domain.enums.PartyStatus;

/** Para listados — sin la lista de contactos (eso sólo va en el detalle). */
public record CompanyResponse(
        Long id,
        String legalName,
        String businessName,
        String cuit,
        String industry,
        String email,
        String phone,
        String locality,
        PartyStatus status,
        Long salesRepId
) {
    public String name() {
        return businessName;
    }
}
