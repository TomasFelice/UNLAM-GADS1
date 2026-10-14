package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.request.CatalogEntryRequest;
import com.ztech.crm.catalogs.dto.request.CreatePipelineRequest;
import com.ztech.crm.catalogs.dto.request.ReorderStagesRequest;
import com.ztech.crm.catalogs.dto.response.PipelineResponse;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.catalogs.service.PipelineService;
import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.validation.PaginationValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/pipelines")
@Tag(name = "Pipelines", description = "Embudos configurables por tenant (ADR-006). Sólo ADMIN escribe.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos o filtros inválidos"),
    @ApiResponse(responseCode = "401", description = "Sesión requerida"),
    @ApiResponse(responseCode = "403", description = "Sólo ADMIN puede configurar"),
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o de otro tenant"),
    @ApiResponse(responseCode = "409", description = "Configuración incompatible o nombre duplicado")
})
public class PipelineController {
    private final PipelineService service;
    private final StageService stages;
    public PipelineController(PipelineService service, StageService stages) { this.service = service; this.stages = stages; }
    @GetMapping @Operation(summary = "Consulta embudos activos e históricos, paginados")
    public PageResponse<PipelineResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name,asc") String sort) {
        return service.list(q, active, PaginationValidator.of(page, size, sort, Set.of("id", "name", "active", "defaultPipeline"), "name"));
    }
    @GetMapping("/{id}") @Operation(summary = "Detalle de un embudo, incluso inactivo")
    public PipelineResponse detail(@PathVariable Long id) { return service.getDetail(id); }
    @GetMapping("/default") @Operation(summary = "Consulta el embudo predeterminado")
    public PipelineResponse defaultPipeline() { return service.getDetail(service.getDefault().id()); }
    @PostMapping @Operation(summary = "Crea un embudo inactivo, vacío o copiando una plantilla")
    @ApiResponse(responseCode = "201", description = "Creado")
    public ResponseEntity<PipelineResponse> create(@Valid @RequestBody CreatePipelineRequest request, UriComponentsBuilder uri) {
        var p = service.create(request);
        return ResponseEntity.created(uri.path("/api/v1/pipelines/{id}").buildAndExpand(p.id()).toUri()).body(p);
    }
    @PutMapping("/{id}") @Operation(summary = "Renombra, activa o desactiva un embudo (ADMIN)")
    public PipelineResponse update(@PathVariable Long id, @Valid @RequestBody CatalogEntryRequest request) { return service.update(id, request); }
    @PostMapping("/{id}/default") @Operation(summary = "Reemplaza el predeterminado de forma atómica (ADMIN)")
    public PipelineResponse makeDefault(@PathVariable Long id) { return service.makeDefault(id); }
    @PostMapping("/{id}/stages/reorder") @Operation(summary = "Aplica el orden completo de etapas de forma atómica (ADMIN)")
    @ApiResponse(responseCode = "422", description = "El orden debe incluir el conjunto completo de etapas, sin duplicados")
    public List<StageResponse> reorder(@PathVariable Long id, @Valid @RequestBody ReorderStagesRequest request) { return stages.reorder(id, request); }
}
