package com.ztech.crm.offerings.repository;

import com.ztech.crm.offerings.domain.Venue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VenueRepository extends JpaRepository<Venue, Long>, JpaSpecificationExecutor<Venue> {

    java.util.List<Venue> findAllByTenantIdAndIdIn(Long tenantId, java.util.Set<Long> ids);

    Optional<Venue> findByIdAndTenantId(Long id, Long tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("select v from Venue v where v.id = :id and v.tenantId = :tenantId")
    Optional<Venue> lockReadable(Long id, Long tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Venue v where v.id = :id and v.tenantId = :tenantId")
    Optional<Venue> lockOwned(Long id, Long tenantId);

    List<Venue> findAllByTenantIdOrderByName(Long tenantId);
}
