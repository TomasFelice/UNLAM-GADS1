package com.ztech.crm.customers.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.domain.Contact;
import com.ztech.crm.customers.dto.request.ContactRequest;
import com.ztech.crm.customers.dto.response.ContactDetailResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import com.ztech.crm.customers.dto.response.ContactSummaryResponse;
import com.ztech.crm.customers.mapper.ContactMapper;
import com.ztech.crm.customers.repository.CompanyRepository;
import com.ztech.crm.customers.repository.ContactRepository;
import com.ztech.crm.customers.repository.specification.ContactSpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.exception.ForbiddenException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.TenantContext;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BE-CUS-04..07. Todo método filtra por el tenant del usuario autenticado (BE-SEC-04). */
@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final ContactMapper contactMapper;
    private final UserService userService;
    private final CustomerVisibilityPort customerVisibilityPort;

    public ContactService(ContactRepository contactRepository, CompanyRepository companyRepository,
                           ContactMapper contactMapper, UserService userService,
                           CustomerVisibilityPort customerVisibilityPort) {
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.contactMapper = contactMapper;
        this.userService = userService;
        this.customerVisibilityPort = customerVisibilityPort;
    }

    @Transactional
    public ContactResponse create(ContactRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        validateDocumentUniqueness(tenantId, request.document(), null);
        Company company = resolveCompany(tenantId, request.companyId());

        Contact contact = new Contact(tenantId, request.firstName(), request.lastName(), request.status());
        applyRequest(contact, company, request, resolveSalesRep(company, request.salesRepId()));

        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        Contact contact = getDirectlyAssignedOrThrow(id, tenantId);
        validateDocumentUniqueness(tenantId, request.document(), id);
        Company company = resolveCompany(tenantId, request.companyId());

        applyRequest(contact, company, request, resolveSalesRep(company, request.salesRepId()));

        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Transactional(readOnly = true)
    public ContactDetailResponse getDetail(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        return contactMapper.toDetailResponse(getReadableOrThrow(id, tenantId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> list(String q, com.ztech.crm.customers.domain.enums.PartyStatus status,
                                               Long originId, Long salesRepId, Long companyId, Pageable pageable) {
        Long tenantId = TenantContext.currentTenantId();
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<Contact> filters = ContactSpecifications.tenant(tenantId);
        if (q != null && !q.isBlank()) filters = filters.and(ContactSpecifications.contains(q));
        if (status != null) filters = filters.and(ContactSpecifications.status(status));
        if (originId != null) filters = filters.and(ContactSpecifications.origin(originId));
        if (salesRepId != null) filters = filters.and(ContactSpecifications.salesRep(salesRepId));
        if (companyId != null) filters = filters.and(ContactSpecifications.company(companyId));
        if (current.role() == Role.SELLER) {
            Set<Long> relatedIds = customerVisibilityPort.findContactIdsWithOwnOpportunities(
                    tenantId, current.userId());
            filters = filters.and(ContactSpecifications.visibleToSeller(current.userId(), relatedIds));
        }
        Page<Contact> page = contactRepository.findAll(filters, pageable);
        return PageResponse.from(page, contactMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Set<Long> findReadableIds() {
        Long tenantId = TenantContext.currentTenantId();
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<Contact> filters = ContactSpecifications.tenant(tenantId);
        if (current.role() == Role.SELLER) {
            filters = filters.and(ContactSpecifications.visibleToSeller(current.userId(),
                    customerVisibilityPort.findContactIdsWithOwnOpportunities(tenantId, current.userId())));
        }
        return contactRepository.findAll(filters).stream().map(Contact::getId).collect(java.util.stream.Collectors.toSet());
    }

    /** Para uso cross-módulo (p. ej. {@code OpportunityService}) — nunca se expone la entidad. */
    @Transactional(readOnly = true)
    public ContactSummaryResponse getSummary(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        Contact contact = getReadableOrThrow(id, tenantId);
        return new ContactSummaryResponse(contact.getId(), contact.getFirstName(), contact.getLastName(),
                contact.getEmail(), contact.getStatus(),
                contact.getCompany() == null ? null : contact.getCompany().getId(), contact.getSalesRepId());
    }

    /** Sólo valida existencia en el tenant — más liviano que {@link #getSummary} cuando no hace falta el DTO. */
    @Transactional(readOnly = true)
    public void assertExists(Long id) {
        getReadableOrThrow(id, TenantContext.currentTenantId());
    }

    private void applyRequest(Contact contact, Company company, ContactRequest request, Long salesRepId) {
        contact.updateDetails(company, request.firstName(), request.lastName(), request.document(),
                request.position(), request.email(), request.phone(), request.status(),
                salesRepId, request.originId(), request.notes());
    }

    private Long resolveSalesRep(Company company, Long requestedId) {
        AuthenticatedUser current = TenantContext.currentUser();
        Long inherited = company == null ? current.userId() : company.getSalesRepId();
        Long resolved = requestedId == null ? inherited : requestedId;
        if (current.role() == Role.SELLER && !current.userId().equals(resolved)) {
            throw new ForbiddenException("SELLER_ASSIGNMENT_FORBIDDEN",
                    "Un vendedor sólo puede asignarse registros a sí mismo.");
        }
        userService.assertActiveAndAssignable(resolved);
        return resolved;
    }

    private Contact getReadableOrThrow(Long id, Long tenantId) {
        Contact contact = getTenantOwnedOrThrow(id, tenantId);
        AuthenticatedUser current = TenantContext.currentUser();
        if (current.role() == Role.SELLER
                && !current.userId().equals(contact.getSalesRepId())
                && !customerVisibilityPort.hasOwnOpportunityForContact(tenantId, current.userId(), id)) {
            throw contactNotFound();
        }
        return contact;
    }

    private Contact getDirectlyAssignedOrThrow(Long id, Long tenantId) {
        Contact contact = getTenantOwnedOrThrow(id, tenantId);
        AuthenticatedUser current = TenantContext.currentUser();
        if (current.role() == Role.SELLER && !current.userId().equals(contact.getSalesRepId())) {
            throw contactNotFound();
        }
        return contact;
    }

    private Contact getTenantOwnedOrThrow(Long id, Long tenantId) {
        return contactRepository.findByIdAndTenantId(id, tenantId).orElseThrow(this::contactNotFound);
    }

    private NotFoundException contactNotFound() {
        return new NotFoundException("No se encontró el contacto solicitado.");
    }

    private Company resolveCompany(Long tenantId, Long companyId) {
        if (companyId == null) {
            return null; // cliente individual (Módulo 1 de la consigna)
        }
        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new NotFoundException("COMPANY_NOT_FOUND", "La empresa relacionada no existe."));
        AuthenticatedUser current = TenantContext.currentUser();
        if (current.role() == Role.SELLER
                && !current.userId().equals(company.getSalesRepId())
                && !customerVisibilityPort.hasOwnOpportunityForCompany(tenantId, current.userId(), companyId)) {
            throw new NotFoundException("COMPANY_NOT_FOUND", "La empresa relacionada no existe.");
        }
        return company;
    }

    private void validateDocumentUniqueness(Long tenantId, String document, Long excludeId) {
        if (document == null || document.isBlank()) {
            return;
        }
        boolean exists = excludeId == null
                ? contactRepository.existsByTenantIdAndDocument(tenantId, document)
                : contactRepository.existsByTenantIdAndDocumentAndIdNot(tenantId, document, excludeId);
        if (exists) {
            throw new ConflictException("DOCUMENT_ALREADY_EXISTS", "Ya existe un contacto con ese documento.");
        }
    }
}
