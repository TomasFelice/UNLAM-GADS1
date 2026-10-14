package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.request.CatalogEntryRequest;
import com.ztech.crm.catalogs.dto.response.ActivityTypeResponse;
import com.ztech.crm.catalogs.service.ActivityTypeService;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.validation.PaginationValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/activity-types")
@Tag(name = "ActivityTypes", description = "Administración de tipo de actividad; lecturas comerciales por tenant")
public class ActivityTypeController {
    private final ActivityTypeService service;
    public ActivityTypeController(ActivityTypeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Lista entradas con búsqueda, actividad y paginación", description = "Tamaño 20, máximo 100. Orden por id, name o active.")
    public PageResponse<ActivityTypeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return service.list(q, active, PaginationValidator.of(page, size, sort, Set.of("id", "name", "active"), "name"));
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consulta una entrada, incluso inactiva")
    @ApiResponse(responseCode = "200", description = "Entrada del tenant")
    @ApiResponse(responseCode = "404", description = "Entrada no encontrada en la organización")
    public ActivityTypeResponse detail(@PathVariable Long id) { return service.getDetail(id); }

    @PostMapping
    @Operation(summary = "Crea un tipo de actividad", description = "Sólo ADMIN. Nombre único por catálogo y tenant.")
    @ApiResponse(responseCode = "201", description = "Entrada creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    public ResponseEntity<ActivityTypeResponse> create(@Valid @RequestBody CatalogEntryRequest request, UriComponentsBuilder uri) {
        var response = service.create(request);
        return ResponseEntity.created(uri.path("/api/v1/activity-types/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }
    @PutMapping("/{id}")
    @Operation(summary = "Edita, activa o da de baja lógica un tipo de actividad", description = "Sólo ADMIN. Conserva ID y referencias históricas; no existe borrado físico.")
    @ApiResponse(responseCode = "200", description = "Entrada actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    @ApiResponse(responseCode = "404", description = "Entrada no encontrada en la organización")
    @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    public ActivityTypeResponse update(@PathVariable Long id, @Valid @RequestBody CatalogEntryRequest request) {
        return service.update(id, request);
    }
}
