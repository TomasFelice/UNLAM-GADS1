package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.ActivityType;
import com.ztech.crm.catalogs.dto.response.ActivityTypeResponse;
import com.ztech.crm.catalogs.repository.ActivityTypeRepository;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityTypeService {
    private final ActivityTypeRepository repository;
    public ActivityTypeService(ActivityTypeRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<ActivityTypeResponse> list() {
        return repository.findAllByTenantIdOrderByName(TenantContext.currentTenantId()).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public ActivityTypeResponse getSummary(Long id) {
        return map(repository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el tipo de actividad solicitado.")));
    }
    private ActivityTypeResponse map(ActivityType value) {
        return new ActivityTypeResponse(value.getId(), value.getName(), value.isActive());
    }
}
