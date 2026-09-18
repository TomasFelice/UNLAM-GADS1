package com.ztech.crm.customers.repository;

import com.ztech.crm.customers.domain.Contact;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {

    Optional<Contact> findByIdAndTenantId(Long id, Long tenantId);

    Page<Contact> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<Contact> findAllByTenantIdAndSalesRepId(Long tenantId, Long salesRepId, Pageable pageable);

    @Query("""
            select contact from Contact contact
            where contact.tenantId = :tenantId
              and (contact.salesRepId = :sellerId or contact.id in :relatedIds)
            """)
    Page<Contact> findAllVisibleToSeller(@Param("tenantId") Long tenantId,
                                         @Param("sellerId") Long sellerId,
                                         @Param("relatedIds") Set<Long> relatedIds,
                                         Pageable pageable);

    List<Contact> findAllByTenantIdAndCompanyId(Long tenantId, Long companyId);

    List<Contact> findAllByTenantIdAndCompanyIdAndSalesRepId(Long tenantId, Long companyId, Long salesRepId);

    @Query("""
            select contact from Contact contact
            where contact.tenantId = :tenantId
              and contact.company.id = :companyId
              and (contact.salesRepId = :sellerId or contact.id in :relatedIds)
            """)
    List<Contact> findAllVisibleToSellerByCompany(@Param("tenantId") Long tenantId,
                                                  @Param("companyId") Long companyId,
                                                  @Param("sellerId") Long sellerId,
                                                  @Param("relatedIds") Set<Long> relatedIds);

    boolean existsByTenantIdAndDocument(Long tenantId, String document);

    boolean existsByTenantIdAndDocumentAndIdNot(Long tenantId, String document, Long id);
}
