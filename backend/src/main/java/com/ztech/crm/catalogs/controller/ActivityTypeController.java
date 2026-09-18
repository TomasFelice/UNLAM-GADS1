package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.response.ActivityTypeResponse;
import com.ztech.crm.catalogs.service.ActivityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity-types")
@Tag(name = "Activity types", description = "Tipos de actividad del tenant")
public class ActivityTypeController {
    private final ActivityTypeService service;
    public ActivityTypeController(ActivityTypeService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista tipos de actividad")
    public List<ActivityTypeResponse> list() { return service.list(); }
}
