package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.response.OriginResponse;
import com.ztech.crm.catalogs.service.OriginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/origins")
@Tag(name = "Origins", description = "Orígenes comerciales del tenant")
public class OriginController {
    private final OriginService service;
    public OriginController(OriginService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista orígenes comerciales")
    public List<OriginResponse> list() { return service.list(); }
}
