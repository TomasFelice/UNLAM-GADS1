package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.response.LossReasonResponse;
import com.ztech.crm.catalogs.service.LossReasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loss-reasons")
@Tag(name = "Loss reasons", description = "Motivos de pérdida del tenant")
public class LossReasonController {
    private final LossReasonService service;
    public LossReasonController(LossReasonService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista motivos de pérdida")
    public List<LossReasonResponse> list() { return service.list(); }
}
