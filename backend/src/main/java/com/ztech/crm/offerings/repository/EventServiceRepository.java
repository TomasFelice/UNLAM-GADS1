package com.ztech.crm.offerings.repository;

import com.ztech.crm.offerings.domain.EventService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventServiceRepository extends JpaRepository<EventService, Long>, JpaSpecificationExecutor<EventService> {

    Optional<EventService> findByIdAndTenantId(Long id, Long tenantId);

    List<EventService> findAllByTenantIdOrderByName(Long tenantId);
}
