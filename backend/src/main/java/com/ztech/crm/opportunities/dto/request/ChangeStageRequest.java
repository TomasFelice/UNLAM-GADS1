package com.ztech.crm.opportunities.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * BE-OPP-05. Sólo mueve entre etapas {@code OPEN} — pasar a una etapa {@code WON}/
 * {@code LOST} se hace con {@code /win}/{@code /lose} (Fase 7), no acá.
 */
public record ChangeStageRequest(

        @Schema(description = "Etapa destino — debe ser una etapa abierta")
        @NotNull(message = "La etapa destino es obligatoria.")
        Long stageId,

        @Schema(description = "Observación opcional sobre el cambio")
        String note,
        @NotNull(message = "La versión leída es obligatoria.")
        @jakarta.validation.constraints.Min(value = 0, message = "La versión no puede ser negativa.")
        Long version
) {
    public ChangeStageRequest(Long stageId, String note) { this(stageId, note, 0L); }
}
