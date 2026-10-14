package com.ztech.crm.opportunities.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignOpportunityRequest(
        @NotNull(message = "El nuevo responsable es obligatorio.") Long salesRepId,
        @NotNull(message = "La versión leída es obligatoria.")
        @Min(value = 0, message = "La versión no puede ser negativa.") Long version) { }
