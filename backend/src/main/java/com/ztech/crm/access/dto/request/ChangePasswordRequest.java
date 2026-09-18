package com.ztech.crm.access.dto.request;

import com.ztech.crm.shared.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String currentPassword,

        @ValidPassword
        String newPassword
) {
}
