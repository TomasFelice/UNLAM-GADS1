package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.LossReason;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LossReasonRepository extends JpaRepository<LossReason, Long>, JpaSpecificationExecutor<LossReason> {
    Optional<LossReason> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndNameAndIdNot(Long tenantId, String name, Long id);
    boolean existsByTenantIdAndName(Long tenantId, String name);
}
