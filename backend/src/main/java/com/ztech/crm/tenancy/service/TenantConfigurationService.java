package com.ztech.crm.tenancy.service;

import com.ztech.crm.tenancy.repository.TenantRepository;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializa configuración del tenant antes de bloquear embudos o etapas. */
@Service
public class TenantConfigurationService {
    private final TenantRepository repository;
    public TenantConfigurationService(TenantRepository repository) { this.repository = repository; }
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockCurrent() {
        repository.lockById(TenantContext.currentTenantId()).orElseThrow();
    }
}
