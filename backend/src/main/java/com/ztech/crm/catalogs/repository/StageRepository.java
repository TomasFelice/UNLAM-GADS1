package com.ztech.crm.catalogs.repository;

import com.ztech.crm.catalogs.domain.Stage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface StageRepository extends JpaRepository<Stage, Long> {

    java.util.List<Stage> findAllByTenantIdAndIdIn(Long tenantId, java.util.Set<Long> ids);

    Optional<Stage> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from Stage s where s.id=:id and s.tenantId=:tenantId")
    Optional<Stage> lockReadable(Long id, Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stage s where s.id=:id and s.tenantId=:tenantId")
    Optional<Stage> lockOwned(Long id, Long tenantId);

    List<Stage> findAllByTenantIdAndPipelineIdOrderByPosition(Long tenantId, Long pipelineId);
    long countByTenantIdAndPipelineIdAndActiveTrueAndKind(Long tenantId, Long pipelineId, com.ztech.crm.catalogs.domain.enums.StageKind kind);

}
