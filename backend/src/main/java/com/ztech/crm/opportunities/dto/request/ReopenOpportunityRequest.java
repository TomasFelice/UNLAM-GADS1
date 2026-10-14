package com.ztech.crm.opportunities.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record ReopenOpportunityRequest(
        @NotNull(message = "La etapa destino es obligatoria.") Long stageId,
        @NotBlank(message = "La nota de reapertura es obligatoria.") String note,
        @NotNull(message = "La versión leída es obligatoria.")
        @Min(value = 0, message = "La versión no puede ser negativa.") Long version) { }
