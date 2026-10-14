package com.ztech.crm.catalogs.service;

/** Implementado por opportunities sin acceso inverso a sus repositorios. */
public interface StageUsagePort {
    boolean isUsed(Long tenantId, Long stageId);
}
