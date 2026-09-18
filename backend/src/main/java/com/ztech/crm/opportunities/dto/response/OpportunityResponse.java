package com.ztech.crm.opportunities.dto.response;

import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Para listados — sólo ids planos, sin resolver nombres de empresa/salón/etapa vía
 * otros `service` (evita N llamadas cross-módulo por página). El detalle enriquecido
 * vive en {@link OpportunityDetailResponse}.
 */
public record OpportunityResponse(
        Long id,
        String title,
        Long companyId,
        Long contactId,
        Long salesRepId,
        Long venueId,
        Long stageId,
        Long eventTypeId,
        OpportunityStatus status,
        BigDecimal estimatedValue,
        Instant eventStart,
        Instant eventEnd,
        Integer attendeeCount,
        Set<Long> serviceIds,
        String companyName,
        String contactName,
        String salesRepName,
        String venueName,
        String stageName
) {
}
