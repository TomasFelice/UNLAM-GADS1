package com.ztech.crm.catalogs.service;

import com.ztech.crm.catalogs.dto.response.LossReasonResponse;
import com.ztech.crm.catalogs.repository.LossReasonRepository;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LossReasonService {
    private final LossReasonRepository repository;
    public LossReasonService(LossReasonRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<LossReasonResponse> list() {
        return repository.findAllByTenantIdOrderByName(TenantContext.currentTenantId()).stream()
                .map(value -> new LossReasonResponse(value.getId(), value.getName(), value.isActive())).toList();
    }
}
