package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.Pipeline;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface PipelineRepository extends JpaRepository<Pipeline, Long>, JpaSpecificationExecutor<Pipeline> {
    Optional<Pipeline> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Pipeline> findByTenantIdAndDefaultPipelineTrue(Long tenantId);
    boolean existsByTenantIdAndName(Long tenantId, String name);
    boolean existsByTenantIdAndNameAndIdNot(Long tenantId, String name, Long id);
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Pipeline p where p.id = :id and p.tenantId = :tenantId")
    Optional<Pipeline> lockReadable(Long id, Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pipeline p where p.id = :id and p.tenantId = :tenantId")
    Optional<Pipeline> lockOwned(Long id, Long tenantId);
}
