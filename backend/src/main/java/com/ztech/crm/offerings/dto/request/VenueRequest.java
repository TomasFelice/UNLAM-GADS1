package com.ztech.crm.offerings.dto.request;

import com.ztech.crm.offerings.domain.enums.VenueStatus;
import com.ztech.crm.shared.validation.OptionalText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record VenueRequest(
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @NotNull(message = "La capacidad es obligatoria.")
        @Positive(message = "La capacidad debe ser mayor a cero.") Integer capacity,
        @Schema(description = "Tarifa opcional en ARS.")
        @DecimalMin(value = "0", message = "La tarifa no puede ser negativa.")
        @Digits(integer = 13, fraction = 2, message = "La tarifa admite hasta 13 enteros y 2 decimales.") BigDecimal rate,
        String address,
        String locality,
        String description,
        List<@NotBlank(message = "Cada elemento de equipamiento debe tener un nombre.") String> equipment,
        @NotNull(message = "El estado es obligatorio.") VenueStatus status
) {
    public VenueRequest {
        name = OptionalText.normalize(name);
        address = OptionalText.normalize(address);
        locality = OptionalText.normalize(locality);
        description = OptionalText.normalize(description);
        equipment = equipment == null ? List.of() : equipment.stream().map(OptionalText::normalize).toList();
    }
}
