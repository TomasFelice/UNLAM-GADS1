package com.ztech.crm.opportunities.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.customers.service.CustomerVisibilityPort;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.specification.OpportunitySpecifications;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Centraliza el alcance comercial de oportunidades e implementa el puerto de clientes. */
@Service
public class OpportunityAccessService implements CustomerVisibilityPort {

    private final OpportunityRepository opportunityRepository;

    public OpportunityAccessService(OpportunityRepository opportunityRepository) {
        this.opportunityRepository = opportunityRepository;
    }

    public Opportunity getReadableOrThrow(Long id) {
        AuthenticatedUser current = TenantContext.currentUser();
        return (current.role() == Role.SELLER
                ? opportunityRepository.findByIdAndTenantIdAndSalesRepId(id, current.tenantId(), current.userId())
                : opportunityRepository.findByIdAndTenantId(id, current.tenantId()))
                .orElseThrow(() -> new NotFoundException("No se encontró la oportunidad solicitada."));
    }

    public Page<Opportunity> findReadable(OpportunityFilter filter, Pageable pageable) {
        return opportunityRepository.findAll(readableSpecification(filter), pageable);
    }

    public List<Opportunity> findAllReadable(OpportunityFilter filter) {
        return opportunityRepository.findAll(readableSpecification(filter),
                org.springframework.data.domain.Sort.by("stageId").ascending().and(
                        org.springframework.data.domain.Sort.by("eventStart").ascending()));
    }

    private Specification<Opportunity> readableSpecification(OpportunityFilter filter) {
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<Opportunity> spec = OpportunitySpecifications.tenant(current.tenantId());
        if (current.role() == Role.SELLER) spec = spec.and(OpportunitySpecifications.salesRep(current.userId()));
        if (filter == null) return spec;
        if (filter.q() != null && !filter.q().isBlank()) spec = spec.and(OpportunitySpecifications.contains(filter.q()));
        if (filter.status() != null) spec = spec.and(OpportunitySpecifications.status(filter.status()));
        if (filter.stageId() != null) spec = spec.and(OpportunitySpecifications.stage(filter.stageId()));
        if (filter.originId() != null) spec = spec.and(OpportunitySpecifications.origin(filter.originId()));
        if (filter.salesRepId() != null) spec = spec.and(OpportunitySpecifications.salesRep(filter.salesRepId()));
        if (filter.venueId() != null) spec = spec.and(OpportunitySpecifications.venue(filter.venueId()));
        if (filter.companyId() != null) spec = spec.and(OpportunitySpecifications.company(filter.companyId()));
        if (filter.contactId() != null) spec = spec.and(OpportunitySpecifications.contact(filter.contactId()));
        if (filter.eventFrom() != null) spec = spec.and(OpportunitySpecifications.eventFrom(filter.eventFrom()));
        if (filter.eventTo() != null) spec = spec.and(OpportunitySpecifications.eventTo(filter.eventTo()));
        return spec;
    }

    public List<Opportunity> findAllReadable() {
        return findAllReadable(null);
    }

    public Set<Long> findReadableIds() {
        return findAllReadable().stream().map(Opportunity::getId).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean hasOwnOpportunityForCompany(Long tenantId, Long sellerId, Long companyId) {
        return opportunityRepository.existsByTenantIdAndSalesRepIdAndCompanyId(tenantId, sellerId, companyId);
    }

    @Override
    public boolean hasOwnOpportunityForContact(Long tenantId, Long sellerId, Long contactId) {
        return opportunityRepository.existsByTenantIdAndSalesRepIdAndContactId(tenantId, sellerId, contactId);
    }

    @Override
    public Set<Long> findCompanyIdsWithOwnOpportunities(Long tenantId, Long sellerId) {
        return opportunityRepository.findCompanyIdsWithOwnOpportunities(tenantId, sellerId);
    }

    @Override
    public Set<Long> findContactIdsWithOwnOpportunities(Long tenantId, Long sellerId) {
        return opportunityRepository.findContactIdsWithOwnOpportunities(tenantId, sellerId);
    }
}
