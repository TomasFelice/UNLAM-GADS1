package com.ztech.crm.opportunities.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record WinOpportunityRequest(
        @NotNull(message = "La etapa destino es obligatoria.") Long stageId,
        @NotNull(message = "El valor final es obligatorio.")
        @DecimalMin(value = "0.00", message = "El valor final no puede ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "El valor final admite hasta 13 enteros y 2 decimales.")
        BigDecimal finalValue,
        String note,
        @NotNull(message = "La versión leída es obligatoria.")
        @Min(value = 0, message = "La versión no puede ser negativa.") Long version) { }
