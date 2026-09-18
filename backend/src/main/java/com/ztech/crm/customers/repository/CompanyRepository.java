package com.ztech.crm.customers.repository;

import com.ztech.crm.customers.domain.Company;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByIdAndTenantId(Long id, Long tenantId);

    Page<Company> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<Company> findAllByTenantIdAndSalesRepId(Long tenantId, Long salesRepId, Pageable pageable);

    @Query("""
            select company from Company company
            where company.tenantId = :tenantId
              and (company.salesRepId = :sellerId or company.id in :relatedIds)
            """)
    Page<Company> findAllVisibleToSeller(@Param("tenantId") Long tenantId,
                                         @Param("sellerId") Long sellerId,
                                         @Param("relatedIds") Set<Long> relatedIds,
                                         Pageable pageable);

    boolean existsByTenantIdAndCuit(Long tenantId, String cuit);

    boolean existsByTenantIdAndCuitAndIdNot(Long tenantId, String cuit, Long id);
}
