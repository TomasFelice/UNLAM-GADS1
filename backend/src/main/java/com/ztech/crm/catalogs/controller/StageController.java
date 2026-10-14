package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.request.StageRequest;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.catalogs.service.StageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/stages")
@Tag(name = "Stages", description = "Etapas propias de un embudo. Sólo ADMIN configura (BE-CAT-02)")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos o filtros inválidos"),
    @ApiResponse(responseCode = "401", description = "Sesión requerida"),
    @ApiResponse(responseCode = "403", description = "Sólo ADMIN puede configurar"),
    @ApiResponse(responseCode = "404", description = "Referencia inexistente o de otro tenant"),
    @ApiResponse(responseCode = "409", description = "Configuración incompatible o nombre duplicado")
})
public class StageController {
    private final StageService service;
    public StageController(StageService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista etapas del embudo elegido o predeterminado, por posición")
    public List<StageResponse> list(@RequestParam(required = false) Long pipelineId) { return service.list(pipelineId); }
    @GetMapping("/{id}") @Operation(summary = "Consulta una etapa, incluso inactiva")
    public StageResponse detail(@PathVariable Long id) { return service.getDetail(id); }
    @PostMapping @Operation(summary = "Crea una etapa al final de su embudo (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Creado")
    public ResponseEntity<StageResponse> create(@Valid @RequestBody StageRequest request, UriComponentsBuilder uri) {
        var stage = service.create(request);
        return ResponseEntity.created(uri.path("/api/v1/stages/{id}").buildAndExpand(stage.id()).toUri()).body(stage);
    }
    @PutMapping("/{id}") @Operation(summary = "Edita o desactiva una etapa conservando su embudo e historia (ADMIN)")
    public StageResponse update(@PathVariable Long id, @Valid @RequestBody StageRequest request) { return service.update(id, request); }
}
