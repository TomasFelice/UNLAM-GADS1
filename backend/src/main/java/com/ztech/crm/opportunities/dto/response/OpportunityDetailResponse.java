package com.ztech.crm.opportunities.dto.response;

import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.customers.dto.response.CompanySummaryResponse;
import com.ztech.crm.customers.dto.response.ContactSummaryResponse;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * BE-OPP-03: cliente, salón y etapa resueltos. {@code salesRepId} queda como id plano
 * — el módulo {@code access} todavía no tiene un {@code UserService} de propósito
 * general para resolver nombres (llega en la Fase 6, BE-ACC-02/03).
 */
public record OpportunityDetailResponse(
        Long id,
        String title,
        CompanySummaryResponse company,
        ContactSummaryResponse contact,
        Long salesRepId,
        VenueResponse venue,
        StageResponse stage,
        Long eventTypeId,
        OpportunityStatus status,
        BigDecimal estimatedValue,
        BigDecimal finalValue,
        Integer probability,
        Instant eventStart,
        Instant eventEnd,
        Integer attendeeCount,
        LocalDate estimatedCloseDate,
        Instant closedAt,
        Long originId,
        Long lossReasonId,
        Set<Long> serviceIds,
        String notes
) {
}
