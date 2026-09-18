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
 * BE-OPP-01. La validación de "empresa o contacto, al menos uno" (RN-02 de la
 * consigna) se hace en el service, no acá — cruza dos campos opcionales.
 */
public record CreateOpportunityRequest(

        @Schema(example = "Evento de fin de año - Empresa X")
        @NotBlank(message = "El título es obligatorio.")
        String title,

        @Schema(description = "Empresa relacionada. Al menos una de companyId/contactId es obligatoria.")
        Long companyId,

        @Schema(description = "Contacto relacionado. Al menos una de companyId/contactId es obligatoria.")
        Long contactId,

        @Schema(description = "Usuario responsable comercial (RN-01 de la consigna)")
        Long salesRepId,

        @NotNull(message = "El salón es obligatorio.")
        Long venueId,

        @Schema(description = "Etapa inicial — debe ser una etapa abierta (DP-06)")
        @NotNull(message = "La etapa inicial es obligatoria.")
        Long stageId,

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

        Set<Long> serviceIds,

        String notes
) {
    public CreateOpportunityRequest(String title, Long companyId, Long contactId, Long salesRepId,
                                    Long venueId, Long stageId, BigDecimal estimatedValue,
                                    Integer probability, Instant eventDate, Integer attendeeCount,
                                    LocalDate estimatedCloseDate, Long originId, String notes) {
        this(title, companyId, contactId, salesRepId, venueId, stageId, 1L, estimatedValue,
                probability, eventDate, eventDate.plusSeconds(86_400), attendeeCount,
                estimatedCloseDate, originId, Set.of(), notes);
    }
}
