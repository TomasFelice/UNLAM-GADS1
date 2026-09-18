package com.ztech.crm.access.controller;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.dto.request.CreateUserRequest;
import com.ztech.crm.access.dto.request.ResetPasswordRequest;
import com.ztech.crm.access.dto.request.UpdateUserRequest;
import com.ztech.crm.access.dto.response.UserResponse;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Administración de usuarios y responsables")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios del tenant (sólo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Listado paginado")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        String[] sortParts = sort.split(",", 2);
        Sort.Direction direction = sortParts.length == 2 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return userService.list(q, role, active,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(direction, sortParts[0])));
    }

    @GetMapping("/assignable")
    @Operation(summary = "Lista responsables disponibles según el rol actual")
    public List<UserResponse> assignable() {
        return userService.listAssignable();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario del tenant (sólo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente o de otro tenant")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Crea un usuario con contraseña temporal (sólo ADMIN)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza perfil, rol y estado (sólo ADMIN)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Restablece la contraseña temporal e invalida sesiones (sólo ADMIN)")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
