package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.domain.Origin;
import com.ztech.crm.catalogs.dto.response.OriginResponse;
import com.ztech.crm.catalogs.repository.OriginRepository;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OriginService {
    private final OriginRepository repository;
    public OriginService(OriginRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<OriginResponse> list() {
        return repository.findAllByTenantIdOrderByName(TenantContext.currentTenantId()).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public OriginResponse getSummary(Long id) {
        return map(repository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el origen solicitado.")));
    }
    private OriginResponse map(Origin value) { return new OriginResponse(value.getId(), value.getName(), value.isActive()); }
}
