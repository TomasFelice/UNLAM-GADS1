package com.ztech.crm.opportunities.controller;

import com.ztech.crm.opportunities.dto.request.ChangeStageRequest;
import com.ztech.crm.opportunities.dto.request.CreateOpportunityRequest;
import com.ztech.crm.opportunities.dto.request.UpdateOpportunityRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityBoardResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityDetailResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.opportunities.service.ChangeStageService;
import com.ztech.crm.opportunities.service.OpportunityService;
import com.ztech.crm.opportunities.service.OpportunityFilter;
import com.ztech.crm.opportunities.service.StageHistoryService;
import com.ztech.crm.opportunities.dto.response.StageHistoryResponse;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.List;
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
@RequestMapping("/api/v1/opportunities")
@Tag(name = "Opportunities", description = "Gestión de oportunidades (BE-OPP-01..03/05/06)")
public class OpportunityController {

    private final com.ztech.crm.opportunities.service.OpportunityLifecycleService lifecycle;
    private final com.ztech.crm.opportunities.service.OpportunityAvailabilityService availability;
    private final OpportunityService opportunityService;
    private final ChangeStageService changeStageService;
    private final StageHistoryService stageHistoryService;

    public OpportunityController(OpportunityService opportunityService, ChangeStageService changeStageService,
                                 StageHistoryService stageHistoryService,
            com.ztech.crm.opportunities.service.OpportunityLifecycleService lifecycle,
            com.ztech.crm.opportunities.service.OpportunityAvailabilityService availability) {
        this.lifecycle = lifecycle;
        this.availability = availability;
        this.opportunityService = opportunityService;
        this.changeStageService = changeStageService;
        this.stageHistoryService = stageHistoryService;
    }

    @PostMapping
    @Operation(summary = "Crea una oportunidad")
    @ApiResponse(responseCode = "201", description = "Oportunidad creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "La empresa, el contacto, el salón o la etapa relacionados no existen")
    @ApiResponse(responseCode = "422", description = "Falta cliente, capacidad excedida o referencia inactiva/no abierta")
    @ApiResponse(responseCode = "409", description = "Embudo inactivo o etapa de otro embudo")
    public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody CreateOpportunityRequest request,
                                                        UriComponentsBuilder uriBuilder) {
        OpportunityResponse response = opportunityService.create(request);
        URI location = uriBuilder.path("/api/v1/opportunities/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una oportunidad abierta (no cambia etapa, responsable ni estado)")
    @ApiResponse(responseCode = "200", description = "Oportunidad actualizada")
    @ApiResponse(responseCode = "404", description = "No existe una oportunidad con ese id")
    @ApiResponse(responseCode = "409", description = "La oportunidad está cerrada o la etapa es incompatible con su embudo/tipo")
    public ResponseEntity<OpportunityResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateOpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una oportunidad, con cliente, salón y etapa resueltos (BE-OPP-03)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe una oportunidad con ese id")
    public ResponseEntity<OpportunityDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getDetail(id));
    }

    @GetMapping
    @Operation(summary = "Lista oportunidades visibles, paginado",
            description = "SELLER ve sólo sus oportunidades; ADMIN y SALES_MANAGER ven todas las del tenant.")
    public ResponseEntity<PageResponse<OpportunityResponse>> list(
            @RequestParam(required = false) Long pipelineId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long originId,
            @RequestParam(required = false) Long salesRepId,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Instant eventFrom,
            @RequestParam(required = false) Instant eventTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "eventStart,desc") String sort) {
        var filter = new OpportunityFilter(q, status, stageId, originId, salesRepId, venueId,
                companyId, contactId, eventFrom, eventTo, pipelineId);
        var pageable = com.ztech.crm.shared.validation.PaginationValidator.of(page, size, sort,
                Set.of("title", "status", "estimatedValue", "eventStart", "estimatedCloseDate", "createdAt"),
                "eventStart");
        return ResponseEntity.ok(opportunityService.list(filter, pageable));
    }

    @GetMapping("/board")
    @Operation(summary = "Oportunidades visibles agrupadas por etapa, una columna por etapa (BE-OPP-06)",
            description = "SELLER ve sólo sus oportunidades; ADMIN y SALES_MANAGER ven todas las del tenant.")
    public ResponseEntity<OpportunityBoardResponse> board(
            @RequestParam(required = false) Long pipelineId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long originId,
            @RequestParam(required = false) Long salesRepId,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Instant eventFrom,
            @RequestParam(required = false) Instant eventTo) {
        return ResponseEntity.ok(opportunityService.getBoard(new OpportunityFilter(q, status, stageId, originId,
                salesRepId, venueId, companyId, contactId, eventFrom, eventTo, pipelineId)));
    }

    @PostMapping("/{id}/stage")
    @Operation(summary = "Cambia la etapa de una oportunidad abierta y registra el historial (BE-OPP-05)")
    @ApiResponse(responseCode = "200", description = "Etapa cambiada")
    @ApiResponse(responseCode = "404", description = "No existe la oportunidad o la etapa destino")
    @ApiResponse(responseCode = "409", description = "La oportunidad está cerrada o la etapa es incompatible con su embudo/tipo")
    @ApiResponse(responseCode = "422", description = "La etapa es igual a la actual")
    public ResponseEntity<OpportunityResponse> changeStage(@PathVariable Long id,
                                                            @Valid @RequestBody ChangeStageRequest request) {
        return ResponseEntity.ok(changeStageService.changeStage(id, request));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Reasigna una oportunidad abierta (ADMIN o responsable comercial)")
    @ApiResponse(responseCode = "200", description = "Oportunidad actualizada")
    @ApiResponse(responseCode = "400", description = "Datos obligatorios o versión inválidos")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o fuera de alcance")
    @ApiResponse(responseCode = "409", description = "Estado, etapa, reserva o versión en conflicto; recargar antes de reintentar")
    @ApiResponse(responseCode = "422", description = "Referencia inactiva, capacidad excedida o salón no disponible")
    public ResponseEntity<OpportunityResponse> assign(@PathVariable Long id,
            @Valid @RequestBody com.ztech.crm.opportunities.dto.request.AssignOpportunityRequest request) {
        return ResponseEntity.ok(lifecycle.assign(id, request));
    }

    @PostMapping("/{id}/win")
    @Operation(summary = "Gana una oportunidad abierta y reserva el salón")
    @ApiResponse(responseCode = "200", description = "Oportunidad actualizada")
    @ApiResponse(responseCode = "400", description = "Datos obligatorios o versión inválidos")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o fuera de alcance")
    @ApiResponse(responseCode = "409", description = "Estado, etapa, reserva o versión en conflicto; recargar antes de reintentar")
    @ApiResponse(responseCode = "422", description = "Referencia inactiva, capacidad excedida o salón no disponible")
    public ResponseEntity<OpportunityResponse> win(@PathVariable Long id,
            @Valid @RequestBody com.ztech.crm.opportunities.dto.request.WinOpportunityRequest request) {
        return ResponseEntity.ok(lifecycle.win(id, request));
    }

    @PostMapping("/{id}/lose")
    @Operation(summary = "Pierde una oportunidad abierta sin reservar")
    @ApiResponse(responseCode = "200", description = "Oportunidad actualizada")
    @ApiResponse(responseCode = "400", description = "Datos obligatorios o versión inválidos")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o fuera de alcance")
    @ApiResponse(responseCode = "409", description = "Estado, etapa, reserva o versión en conflicto; recargar antes de reintentar")
    @ApiResponse(responseCode = "422", description = "Referencia inactiva, capacidad excedida o salón no disponible")
    public ResponseEntity<OpportunityResponse> lose(@PathVariable Long id,
            @Valid @RequestBody com.ztech.crm.opportunities.dto.request.LoseOpportunityRequest request) {
        return ResponseEntity.ok(lifecycle.lose(id, request));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabre y conserva cierres anteriores (ADMIN o responsable comercial)")
    @ApiResponse(responseCode = "200", description = "Oportunidad actualizada")
    @ApiResponse(responseCode = "400", description = "Datos obligatorios o versión inválidos")
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o fuera de alcance")
    @ApiResponse(responseCode = "409", description = "Estado, etapa, reserva o versión en conflicto; recargar antes de reintentar")
    @ApiResponse(responseCode = "422", description = "Referencia inactiva, capacidad excedida o salón no disponible")
    public ResponseEntity<OpportunityResponse> reopen(@PathVariable Long id,
            @Valid @RequestBody com.ztech.crm.opportunities.dto.request.ReopenOpportunityRequest request) {
        return ResponseEntity.ok(lifecycle.reopen(id, request));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Consulta disponibilidad actual del salón para la oportunidad; ganar vuelve a validarla")
    public ResponseEntity<com.ztech.crm.opportunities.dto.response.VenueAvailabilityResponse> availability(@PathVariable Long id) {
        return ResponseEntity.ok(availability.get(id));
    }

    @GetMapping("/{id}/stage-history")
    @Operation(summary = "Lista el historial append-only de etapas en orden cronológico")
    public ResponseEntity<List<StageHistoryResponse>> stageHistory(@PathVariable Long id) {
        return ResponseEntity.ok(stageHistoryService.list(id));
    }
}
