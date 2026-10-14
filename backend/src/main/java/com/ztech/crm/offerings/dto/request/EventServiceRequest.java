package com.ztech.crm.offerings.dto.request;

import com.ztech.crm.shared.validation.OptionalText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EventServiceRequest(
        @NotBlank(message = "El nombre es obligatorio.") String name,
        String description,
        @Schema(description = "Precio opcional en ARS.")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "El precio admite hasta 13 enteros y 2 decimales.") BigDecimal price,
        @NotNull(message = "Indicá si el servicio está activo.") Boolean active
) {
    public EventServiceRequest {
        name = OptionalText.normalize(name);
        description = OptionalText.normalize(description);
    }
}
