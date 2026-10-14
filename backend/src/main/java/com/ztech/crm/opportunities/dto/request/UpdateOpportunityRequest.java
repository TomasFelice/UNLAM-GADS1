package com.ztech.crm.opportunities.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * BE-OPP-02. A propósito NO incluye {@code stageId} (cambia por
 * {@code POST /{id}/stage}, Fase 4), {@code salesRepId} (cambia por
 * {@code POST /{id}/assign}, Fase 7) ni {@code status} (cambia por
 * {@code /win} o {@code /lose}) — ninguno de esos es una edición genérica
 * (docs/arquitectura/02-modulos-y-capas.md).
 */
public record UpdateOpportunityRequest(

        @NotBlank(message = "El título es obligatorio.")
        String title,

        Long companyId,

        Long contactId,

        @NotNull(message = "El salón es obligatorio.")
        Long venueId,

        @NotNull(message = "El tipo de evento es obligatorio.")
        Long eventTypeId,

        BigDecimal estimatedValue,

        @Min(value = 0, message = "La probabilidad no puede ser negativa.")
        @Max(value = 100, message = "La probabilidad no puede superar 100.")
        Integer probability,

        @NotNull(message = "El inicio del evento es obligatorio.")
        Instant eventStart,

        @NotNull(message = "El fin del evento es obligatorio.")
        Instant eventEnd,

        @NotNull(message = "La cantidad de asistentes es obligatoria.")
        @Positive(message = "La cantidad de asistentes debe ser mayor a cero.")
        Integer attendeeCount,

        LocalDate estimatedCloseDate,

        Long originId,

        @Schema(description = "Servicios asociados. Omitido o null conserva los actuales; [] los quita. Sólo nuevas asociaciones requieren servicio activo.")
        Set<Long> serviceIds,

        String notes,

        @Schema(description = "Embudo comercial. En alta, omitido usa el predeterminado; en edición no puede cambiar.")
        Long pipelineId,

        @NotNull(message = "La versión leída es obligatoria.")
        @Min(value = 0, message = "La versión no puede ser negativa.")
        Long version
) {
    public UpdateOpportunityRequest(String title, Long companyId, Long contactId, Long venueId, Long eventTypeId,
            BigDecimal estimatedValue, Integer probability, Instant eventStart, Instant eventEnd, Integer attendeeCount,
            LocalDate estimatedCloseDate, Long originId, Set<Long> serviceIds, String notes, Long pipelineId) {
        this(title, companyId, contactId, venueId, eventTypeId, estimatedValue, probability, eventStart, eventEnd,
                attendeeCount, estimatedCloseDate, originId, serviceIds, notes, pipelineId, 0L);
    }

    public UpdateOpportunityRequest(String title, Long companyId, Long contactId, Long venueId, Long eventTypeId,
            BigDecimal estimatedValue, Integer probability, Instant eventStart, Instant eventEnd, Integer attendeeCount,
            LocalDate estimatedCloseDate, Long originId, Set<Long> serviceIds, String notes) {
        this(title, companyId, contactId, venueId, eventTypeId, estimatedValue, probability, eventStart, eventEnd,
                attendeeCount, estimatedCloseDate, originId, serviceIds, notes, null, 0L);
    }

    public UpdateOpportunityRequest(String title, Long companyId, Long contactId, Long venueId,
                                    BigDecimal estimatedValue, Integer probability, Instant eventDate,
                                    Integer attendeeCount, LocalDate estimatedCloseDate, Long originId,
                                    String notes) {
        this(title, companyId, contactId, venueId, 1L, estimatedValue, probability, eventDate,
                eventDate.plusSeconds(86_400), attendeeCount, estimatedCloseDate, originId, Set.of(), notes);
    }
}
