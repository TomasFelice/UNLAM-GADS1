package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.ActivityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    List<ActivityType> findAllByTenantIdOrderByName(Long tenantId);
    Optional<ActivityType> findByIdAndTenantId(Long id, Long tenantId);
}
