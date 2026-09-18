package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {

    Optional<EventType> findByIdAndTenantId(Long id, Long tenantId);

    List<EventType> findAllByTenantIdOrderByName(Long tenantId);
}
