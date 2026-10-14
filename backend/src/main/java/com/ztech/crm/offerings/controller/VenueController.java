package com.ztech.crm.offerings.controller;

import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.offerings.dto.request.VenueRequest;
import com.ztech.crm.offerings.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import com.ztech.crm.offerings.domain.enums.VenueStatus;
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

/** BE-OFF-01/02: lecturas comerciales y escrituras administrativas. */
@RestController
@RequestMapping("/api/v1/venues")
@Tag(name = "Venues", description = "Salones (BE-OFF-01/02)")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    @Operation(summary = "Crea un salón", description = "Sólo ADMIN; tenant y actor se resuelven de la sesión.")
    @ApiResponse(responseCode = "201", description = "Salón creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest request, UriComponentsBuilder uri) {
        VenueResponse response = venueService.create(request);
        return ResponseEntity.created(uri.path("/api/v1/venues/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita un salón o cambia su estado", description = "Sólo ADMIN; la baja es lógica y conserva referencias comerciales.")
    @ApiResponse(responseCode = "200", description = "Salón actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Requiere ADMIN")
    @ApiResponse(responseCode = "404", description = "No se encontró el salón en la organización")
    @ApiResponse(responseCode = "409", description = "La reducción de capacidad invalidaría una reserva confirmada")
    public ResponseEntity<VenueResponse> update(@PathVariable Long id, @Valid @RequestBody VenueRequest request) {
        return ResponseEntity.ok(venueService.update(id, request));
    }

    @GetMapping
    @Operation(summary = "Lista salones del tenant con búsqueda, estado y paginación")
    public ResponseEntity<PageResponse<VenueResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VenueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        var pageable = PaginationValidator.of(page, size, sort,
                Set.of("id", "name", "capacity", "rate", "status", "locality", "createdAt"), "name");
        return ResponseEntity.ok(venueService.list(q, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un salón")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe un salón con ese id")
    public ResponseEntity<VenueResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(venueService.getDetail(id));
    }
}
