package com.ztech.crm.customers.service;

import java.util.Set;

/**
 * Puerto invertido para consultar relaciones comerciales sin hacer que
 * {@code customers} dependa del dominio o repositorio de {@code opportunities}.
 */
public interface CustomerVisibilityPort {

    boolean hasOwnOpportunityForCompany(Long tenantId, Long sellerId, Long companyId);

    boolean hasOwnOpportunityForContact(Long tenantId, Long sellerId, Long contactId);

    Set<Long> findCompanyIdsWithOwnOpportunities(Long tenantId, Long sellerId);

    Set<Long> findContactIdsWithOwnOpportunities(Long tenantId, Long sellerId);
}
