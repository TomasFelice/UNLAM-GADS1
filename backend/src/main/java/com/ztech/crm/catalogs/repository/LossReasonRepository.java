package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.LossReason;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LossReasonRepository extends JpaRepository<LossReason, Long> {
    List<LossReason> findAllByTenantIdOrderByName(Long tenantId);
}
