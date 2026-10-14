package com.ztech.crm.offerings.controller;

import com.ztech.crm.offerings.dto.response.EventServiceResponse;
import com.ztech.crm.offerings.dto.request.EventServiceRequest;
import com.ztech.crm.offerings.service.EventServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.validation.PaginationValidator;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BE-OFF-03/04: lecturas comerciales y escrituras administrativas. */
@RestController
@RequestMapping("/api/v1/event-services")
@Tag(name = "EventServices", description = "Servicios adicionales (BE-OFF-03/04)")
public class EventServiceController {

    private final EventServiceService eventServiceService;

    public EventServiceController(EventServiceService eventServiceService) {
        this.eventServiceService = eventServiceService;
    }

    @PostMapping
    @Operation(summary = "Crea un servicio adicional", description = "Sólo ADMIN; tenant y actor se resuelven de la sesión.")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    public ResponseEntity<EventServiceResponse> create(@Valid @RequestBody EventServiceRequest request, UriComponentsBuilder uri) {
        EventServiceResponse response = eventServiceService.create(request);
        return ResponseEntity.created(uri.path("/api/v1/event-services/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita un servicio o cambia su actividad", description = "Sólo ADMIN; la baja es lógica y conserva referencias comerciales.")
    @ApiResponse(responseCode = "200", description = "Servicio actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    @ApiResponse(responseCode = "404", description = "No se encontró el servicio en la organización")
    public ResponseEntity<EventServiceResponse> update(@PathVariable Long id, @Valid @RequestBody EventServiceRequest request) {
        return ResponseEntity.ok(eventServiceService.update(id, request));
    }

    @GetMapping
    @Operation(summary = "Lista servicios del tenant con búsqueda, actividad y paginación")
    public ResponseEntity<PageResponse<EventServiceResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        var pageable = PaginationValidator.of(page, size, sort,
                Set.of("id", "name", "price", "active", "createdAt"), "name");
        return ResponseEntity.ok(eventServiceService.list(q, active, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un servicio adicional")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe un servicio con ese id")
    public ResponseEntity<EventServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventServiceService.getDetail(id));
    }
}
