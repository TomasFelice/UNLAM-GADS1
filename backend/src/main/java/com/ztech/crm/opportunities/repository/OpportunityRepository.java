package com.ztech.crm.opportunities.repository;

import com.ztech.crm.opportunities.domain.Opportunity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    Optional<Opportunity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Opportunity> findByIdAndTenantIdAndSalesRepId(Long id, Long tenantId, Long salesRepId);

    Page<Opportunity> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<Opportunity> findAllByTenantIdAndSalesRepId(Long tenantId, Long salesRepId, Pageable pageable);

    /** Sin paginar — para el tablero (BE-OPP-06), que necesita todo de una vez. */
    List<Opportunity> findAllByTenantId(Long tenantId);

    List<Opportunity> findAllByTenantIdAndSalesRepId(Long tenantId, Long salesRepId);

    boolean existsByTenantIdAndSalesRepIdAndCompanyId(Long tenantId, Long salesRepId, Long companyId);

    boolean existsByTenantIdAndSalesRepIdAndContactId(Long tenantId, Long salesRepId, Long contactId);

    @Query("""
            select distinct opportunity.companyId from Opportunity opportunity
            where opportunity.tenantId = :tenantId
              and opportunity.salesRepId = :sellerId
              and opportunity.companyId is not null
            """)
    Set<Long> findCompanyIdsWithOwnOpportunities(@Param("tenantId") Long tenantId,
                                                 @Param("sellerId") Long sellerId);

    @Query("""
            select distinct opportunity.contactId from Opportunity opportunity
            where opportunity.tenantId = :tenantId
              and opportunity.salesRepId = :sellerId
              and opportunity.contactId is not null
            """)
    Set<Long> findContactIdsWithOwnOpportunities(@Param("tenantId") Long tenantId,
                                                 @Param("sellerId") Long sellerId);
}
