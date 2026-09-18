package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.Origin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginRepository extends JpaRepository<Origin, Long> {
    List<Origin> findAllByTenantIdOrderByName(Long tenantId);
    Optional<Origin> findByIdAndTenantId(Long id, Long tenantId);
}
