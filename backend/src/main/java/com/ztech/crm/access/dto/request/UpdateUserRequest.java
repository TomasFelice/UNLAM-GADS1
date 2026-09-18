package com.ztech.crm.access.dto.request;

import com.ztech.crm.access.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        @NotBlank(message = "El nombre es obligatorio.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        String lastName,

        @NotNull(message = "El rol es obligatorio.")
        Role role,

        boolean active
) {
}
