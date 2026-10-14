package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.EventType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventTypeRepository extends JpaRepository<EventType, Long>, JpaSpecificationExecutor<EventType> {
    Optional<EventType> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndNameAndIdNot(Long tenantId, String name, Long id);
    boolean existsByTenantIdAndName(Long tenantId, String name);
}
