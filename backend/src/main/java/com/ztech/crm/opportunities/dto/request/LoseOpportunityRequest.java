package com.ztech.crm.opportunities.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LoseOpportunityRequest(
        @NotNull(message = "La etapa destino es obligatoria.") Long stageId,
        @NotNull(message = "El motivo de pérdida es obligatorio.") Long lossReasonId,
        String note,
        @NotNull(message = "La versión leída es obligatoria.")
        @Min(value = 0, message = "La versión no puede ser negativa.") Long version) { }
