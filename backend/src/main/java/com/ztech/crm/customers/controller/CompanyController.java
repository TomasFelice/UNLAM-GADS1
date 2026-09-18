package com.ztech.crm.customers.controller;

import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.service.CompanyService;
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
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Gestión de empresas (BE-CUS-01..03)")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @Operation(summary = "Crea una empresa")
    @ApiResponse(responseCode = "201", description = "Empresa creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Ya existe una empresa con ese CUIT")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CompanyResponse response = companyService.create(request);
        URI location = uriBuilder.path("/api/v1/companies/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una empresa")
    @ApiResponse(responseCode = "200", description = "Empresa actualizada")
    @ApiResponse(responseCode = "404", description = "No existe una empresa con ese id")
    @ApiResponse(responseCode = "409", description = "Ya existe una empresa con ese CUIT")
    public ResponseEntity<CompanyResponse> update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una empresa, con sus contactos (BE-CUS-03)",
            description = "Para SELLER exige asignación directa o una oportunidad propia; los contactos anidados también se filtran por ese alcance.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe una empresa con ese id")
    public ResponseEntity<CompanyDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getDetail(id));
    }

    @GetMapping
    @Operation(summary = "Lista empresas visibles, paginado",
            description = "ADMIN y SALES_MANAGER ven el tenant completo. SELLER ve asignaciones directas y empresas relacionadas con oportunidades propias.")
    public ResponseEntity<PageResponse<CompanyResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PartyStatus status,
            @RequestParam(required = false) Long originId,
            @RequestParam(required = false) Long salesRepId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "businessName,asc") String sort) {
        var pageable = com.ztech.crm.shared.validation.PaginationValidator.of(page, size, sort,
                Set.of("businessName", "legalName", "status", "locality", "createdAt"), "businessName");
        return ResponseEntity.ok(companyService.list(q, status, originId, salesRepId, pageable));
    }
}
