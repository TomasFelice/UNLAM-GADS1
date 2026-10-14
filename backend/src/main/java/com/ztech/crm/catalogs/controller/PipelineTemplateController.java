package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.response.PipelineTemplateResponse;
import com.ztech.crm.catalogs.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline-templates")
@Tag(name = "PipelineTemplates", description = "Plantillas inmutables de la aplicación; cada copia usa IDs propios")
public class PipelineTemplateController {
    private final PipelineService service;
    public PipelineTemplateController(PipelineService service) { this.service = service; }
    @GetMapping @Operation(summary = "Previsualiza las plantillas de embudo disponibles")
    public List<PipelineTemplateResponse> list() { return service.templates(); }
}
