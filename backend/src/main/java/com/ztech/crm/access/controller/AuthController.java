package com.ztech.crm.access.controller;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.request.ChangePasswordRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.access.service.AuthService;
import com.ztech.crm.access.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticación (BE-SEC-01)")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @SecurityRequirements // endpoint público — sin este override heredaría el bearerAuth global de OpenApiConfig
    @Operation(summary = "Inicia sesión y devuelve un JWT")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Cambia la contraseña del usuario autenticado e invalida sus sesiones")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "422", description = "La contraseña actual es incorrecta o no cambió")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
        return ResponseEntity.noContent().build();
    }
}
