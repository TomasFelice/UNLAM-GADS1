package com.ztech.crm.opportunities.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrato modular de lectura; sin entidades ni DTOs de presentación. */
public record OpportunitySummaryContext(Long id, Long version, Long salesRepId, String title, String notes,
        String status, String stage, String stageKind, String pipeline, String venue, Integer capacity,
        BigDecimal venueRate, List<String> services, String eventType, Instant eventStart, Instant eventEnd,
        Integer attendees, BigDecimal estimatedValue, BigDecimal finalValue, Integer probability,
        LocalDate estimatedCloseDate, Instant closedAt, String lossReason, long transitionCount,
        List<Transition> transitions) {
    public record Transition(Long id, Instant changedAt, String eventType, String fromStage,
            String toStage, String destinationStatus, boolean snapshotComplete, BigDecimal finalValue,
            Instant closedAt, String lossReason, String note) { }
}
