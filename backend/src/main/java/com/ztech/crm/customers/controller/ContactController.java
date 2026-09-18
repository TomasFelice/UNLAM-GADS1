package com.ztech.crm.customers.controller;

import com.ztech.crm.customers.dto.request.ContactRequest;
import com.ztech.crm.customers.dto.response.ContactDetailResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.service.ContactService;
import com.ztech.crm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Gestión de contactos (BE-CUS-04..07)")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @Operation(summary = "Crea un contacto")
    @ApiResponse(responseCode = "201", description = "Contacto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "La empresa relacionada no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe un contacto con ese documento")
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        ContactResponse response = contactService.create(request);
        URI location = uriBuilder.path("/api/v1/contacts/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita un contacto")
    @ApiResponse(responseCode = "200", description = "Contacto actualizado")
    @ApiResponse(responseCode = "404", description = "No existe un contacto con ese id, o la empresa relacionada no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe un contacto con ese documento")
    public ResponseEntity<ContactResponse> update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un contacto",
            description = "Para SELLER exige asignación directa o una oportunidad propia relacionada.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe un contacto con ese id")
    public ResponseEntity<ContactDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getDetail(id));
    }

    @GetMapping
    @Operation(summary = "Lista contactos visibles, paginado",
            description = "ADMIN y SALES_MANAGER ven el tenant completo. SELLER ve asignaciones directas y contactos relacionados con oportunidades propias.")
    public ResponseEntity<PageResponse<ContactResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PartyStatus status,
            @RequestParam(required = false) Long originId,
            @RequestParam(required = false) Long salesRepId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort) {
        var pageable = com.ztech.crm.shared.validation.PaginationValidator.of(page, size, sort,
                Set.of("firstName", "lastName", "status", "createdAt"), "lastName");
        return ResponseEntity.ok(contactService.list(q, status, originId, salesRepId, companyId, pageable));
    }
}
