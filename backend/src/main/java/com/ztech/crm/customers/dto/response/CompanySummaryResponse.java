package com.ztech.crm.customers.dto.response;

/** Resumen de una empresa, embebido en {@link ContactDetailResponse}. */
public record CompanySummaryResponse(Long id, String legalName, String businessName, Long salesRepId) {
}
