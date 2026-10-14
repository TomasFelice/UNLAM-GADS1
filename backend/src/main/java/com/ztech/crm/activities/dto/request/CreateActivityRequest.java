package com.ztech.crm.activities.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

public record CreateActivityRequest(
        @NotNull(message = "El tipo de actividad es obligatorio.") Long typeId,
        @NotNull(message = "La fecha y hora del hecho son obligatorias.")
        @PastOrPresent(message = "La actividad debe haber ocurrido; no puede tener una fecha futura.") Instant occurredAt,
        Long companyId, Long contactId, Long opportunityId,
        @NotBlank(message = "La descripción es obligatoria.") String description,
        @NotBlank(message = "El resultado es obligatorio.") String result) { }
