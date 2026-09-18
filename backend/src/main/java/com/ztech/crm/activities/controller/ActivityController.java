package com.ztech.crm.activities.controller;

import com.ztech.crm.activities.dto.response.ActivityResponse;
import com.ztech.crm.activities.service.ActivityService;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.validation.PaginationValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Activities", description = "Lectura tenant-aware del historial comercial")
public class ActivityController {
    private static final Set<String> SORTS = Set.of("occurredAt", "createdAt", "id");
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service = service; }

    @GetMapping("/api/v1/activities")
    @Operation(summary = "Lista actividades visibles, ordenadas y paginadas")
    public PageResponse<ActivityResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        return service.list(pageable(page, size, sort));
    }

    @GetMapping("/api/v1/companies/{id}/activities")
    @Operation(summary = "Lista actividades de una empresa visible")
    public PageResponse<ActivityResponse> byCompany(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        return service.byCompany(id, pageable(page, size, sort));
    }

    @GetMapping("/api/v1/contacts/{id}/activities")
    @Operation(summary = "Lista actividades de un contacto visible")
    public PageResponse<ActivityResponse> byContact(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        return service.byContact(id, pageable(page, size, sort));
    }

    @GetMapping("/api/v1/opportunities/{id}/activities")
    @Operation(summary = "Lista actividades de una oportunidad visible")
    public PageResponse<ActivityResponse> byOpportunity(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        return service.byOpportunity(id, pageable(page, size, sort));
    }

    private Pageable pageable(int page, int size, String sort) {
        return PaginationValidator.of(page, size, sort, SORTS, "occurredAt");
    }
}
