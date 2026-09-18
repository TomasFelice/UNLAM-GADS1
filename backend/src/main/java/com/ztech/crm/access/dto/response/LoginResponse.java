package com.ztech.crm.access.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(

        @Schema(description = "JWT a enviar como 'Authorization: Bearer <token>' en el resto de los requests")
        String accessToken,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos hasta que el token expira", example = "28800")
        long expiresInSeconds,

        @Schema(description = "Indica que sólo se permite cambiar la contraseña antes de continuar")
        boolean mustChangePassword,

        @Schema(description = "Datos mínimos del usuario autenticado, para que el frontend no tenga que decodificar el JWT")
        UserSummary user
) {

    public record UserSummary(Long id, String email, String firstName, String lastName, String role) {
    }
}
