package com.ztech.crm.catalogs.controller;

import com.ztech.crm.catalogs.dto.response.EventTypeResponse;
import com.ztech.crm.catalogs.service.EventTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event-types")
@Tag(name = "EventTypes", description = "Tipos configurables de evento")
public class EventTypeController {

    private final EventTypeService eventTypeService;

    public EventTypeController(EventTypeService eventTypeService) {
        this.eventTypeService = eventTypeService;
    }

    @GetMapping
    @Operation(summary = "Lista los tipos de evento del tenant")
    public ResponseEntity<List<EventTypeResponse>> list() {
        return ResponseEntity.ok(eventTypeService.list());
    }
}
