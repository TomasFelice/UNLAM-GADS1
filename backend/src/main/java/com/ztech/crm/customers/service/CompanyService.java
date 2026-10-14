package com.ztech.crm.customers.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.catalogs.service.OriginService;
import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.dto.response.CompanySummaryResponse;
import com.ztech.crm.customers.mapper.CompanyMapper;
import com.ztech.crm.customers.repository.CompanyRepository;
import com.ztech.crm.customers.repository.specification.CompanySpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.exception.ForbiddenException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.TenantContext;
import com.ztech.crm.shared.validation.OptionalText;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BE-CUS-01..03. Todo método filtra por el tenant del usuario autenticado (BE-SEC-04). */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserService userService;
    private final OriginService originService;
    private final CustomerVisibilityPort customerVisibilityPort;

    public CompanyService(CompanyRepository companyRepository,
                           CompanyMapper companyMapper, UserService userService,
                           CustomerVisibilityPort customerVisibilityPort, OriginService originService) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.userService = userService;
        this.originService = originService;
        this.customerVisibilityPort = customerVisibilityPort;
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        validateCuitUniqueness(tenantId, request.cuit(), null);

        Long salesRepId = resolveSalesRep(request.salesRepId());
        Company company = new Company(tenantId, request.legalName(), request.businessName(),
                request.status(), salesRepId);
        applyRequest(company, request, salesRepId);

        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        Company company = getDirectlyAssignedOrThrow(id, tenantId);
        validateCuitUniqueness(tenantId, request.cuit(), id);

        applyRequest(company, request, resolveSalesRep(request.salesRepId()));

        return toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyDetailResponse getDetail(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        Company company = getReadableOrThrow(id, tenantId);
        return companyMapper.toDetailResponse(company, userService.getIdentity(company.getSalesRepId()).fullName());
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(String q, com.ztech.crm.customers.domain.enums.PartyStatus status,
                                               Long originId, Long salesRepId, Pageable pageable) {
        Long tenantId = TenantContext.currentTenantId();
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<Company> filters = CompanySpecifications.tenant(tenantId);
        if (q != null && !q.isBlank()) filters = filters.and(CompanySpecifications.contains(q));
        if (status != null) filters = filters.and(CompanySpecifications.status(status));
        if (originId != null) filters = filters.and(CompanySpecifications.origin(originId));
        if (salesRepId != null) filters = filters.and(CompanySpecifications.salesRep(salesRepId));
        if (current.role() == Role.SELLER) {
            Set<Long> relatedIds = customerVisibilityPort.findCompanyIdsWithOwnOpportunities(
                    tenantId, current.userId());
            filters = filters.and(CompanySpecifications.visibleToSeller(current.userId(), relatedIds));
        }
        Page<Company> page = companyRepository.findAll(filters, pageable);
        var responsibleIds = page.stream().map(Company::getSalesRepId).collect(java.util.stream.Collectors.toSet());
        var identities = userService.getIdentities(responsibleIds);
        return PageResponse.from(page, company -> companyMapper.toResponse(company,
                identities.get(company.getSalesRepId()).fullName()));
    }

    @Transactional(readOnly = true)
    public Set<Long> findReadableIds() {
        Long tenantId = TenantContext.currentTenantId();
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<Company> filters = CompanySpecifications.tenant(tenantId);
        if (current.role() == Role.SELLER) {
            filters = filters.and(CompanySpecifications.visibleToSeller(current.userId(),
                    customerVisibilityPort.findCompanyIdsWithOwnOpportunities(tenantId, current.userId())));
        }
        return companyRepository.findAll(filters).stream().map(Company::getId).collect(java.util.stream.Collectors.toSet());
    }

    /** Para uso cross-módulo (p. ej. {@code OpportunityService}) — nunca se expone la entidad. */
    @Transactional(readOnly = true)
    public CompanySummaryResponse getSummary(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        Company company = getReadableOrThrow(id, tenantId);
        return new CompanySummaryResponse(company.getId(), company.getLegalName(),
                company.getBusinessName(), company.getSalesRepId());
    }

    /** Sólo valida existencia en el tenant — más liviano que {@link #getSummary} cuando no hace falta el DTO. */
    @Transactional(readOnly = true)
    public void assertExists(Long id) {
        getReadableOrThrow(id, TenantContext.currentTenantId());
    }

    private CompanyResponse toResponse(Company company) {
        return companyMapper.toResponse(company, userService.getIdentity(company.getSalesRepId()).fullName());
    }

    /** Lectura en lote con el mismo alcance comercial que el detalle; sin DTO ni entidad ajenos. */
    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getNames(java.util.Set<Long> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        var current = TenantContext.currentUser();
        Specification<Company> filters = CompanySpecifications.tenant(current.tenantId());
        filters = filters.and((root, query, builder) -> root.get("id").in(ids));
        if (current.role() == Role.SELLER) {
            filters = filters.and(CompanySpecifications.visibleToSeller(current.userId(),
                    customerVisibilityPort.findCompanyIdsWithOwnOpportunities(current.tenantId(), current.userId())));
        }
        return companyRepository.findAll(filters).stream().collect(java.util.stream.Collectors.toMap(
                Company::getId, value -> value.getBusinessName()));
    }

    private void applyRequest(Company company, CompanyRequest request, Long salesRepId) {
        originService.assertSelectable(request.originId(), company.getOriginId());
        company.updateDetails(request.legalName().strip(), request.businessName().strip(),
                OptionalText.canonicalCuit(request.cuit()), normalizeBlank(request.industry()), normalizeBlank(request.email()),
                normalizeBlank(request.phone()), normalizeBlank(request.address()), normalizeBlank(request.locality()),
                normalizeBlank(request.website()), request.status(), salesRepId,
                request.originId(), normalizeBlank(request.notes()));
    }

    private Long resolveSalesRep(Long requestedId) {
        AuthenticatedUser current = TenantContext.currentUser();
        Long resolved = requestedId == null ? current.userId() : requestedId;
        if (current.role() == Role.SELLER && !current.userId().equals(resolved)) {
            throw new ForbiddenException("SELLER_ASSIGNMENT_FORBIDDEN",
                    "Un vendedor sólo puede asignarse registros a sí mismo.");
        }
        userService.assertActiveAndAssignable(resolved);
        return resolved;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** Escritura de hechos sobre asignación directa; la lectura indirecta no la habilita. */
    @Transactional(readOnly = true)
    public CustomerActivityContext getForActivity(Long id) {
        var value = getDirectlyAssignedOrThrow(id, TenantContext.currentTenantId());
        return new CustomerActivityContext(value.getId(), value.getId(), value.getSalesRepId());
    }

    private Company getReadableOrThrow(Long id, Long tenantId) {
        // 404, nunca 403: no confirma si el id existe en otro tenant (BE-SEC-04).
        Company company = getTenantOwnedOrThrow(id, tenantId);
        AuthenticatedUser current = TenantContext.currentUser();
        if (current.role() == Role.SELLER
                && !current.userId().equals(company.getSalesRepId())
                && !customerVisibilityPort.hasOwnOpportunityForCompany(tenantId, current.userId(), id)) {
            throw companyNotFound();
        }
        return company;
    }

    private Company getDirectlyAssignedOrThrow(Long id, Long tenantId) {
        Company company = getTenantOwnedOrThrow(id, tenantId);
        AuthenticatedUser current = TenantContext.currentUser();
        if (current.role() == Role.SELLER && !current.userId().equals(company.getSalesRepId())) {
            throw companyNotFound();
        }
        return company;
    }

    private Company getTenantOwnedOrThrow(Long id, Long tenantId) {
        return companyRepository.findByIdAndTenantId(id, tenantId).orElseThrow(this::companyNotFound);
    }

    private NotFoundException companyNotFound() {
        return new NotFoundException("No se encontró la empresa solicitada.");
    }

    private void validateCuitUniqueness(Long tenantId, String cuit, Long excludeId) {
        cuit = OptionalText.canonicalCuit(cuit);
        if (cuit == null || cuit.isBlank()) {
            return;
        }
        boolean exists = excludeId == null
                ? companyRepository.existsByTenantIdAndCuit(tenantId, cuit)
                : companyRepository.existsByTenantIdAndCuitAndIdNot(tenantId, cuit, excludeId);
        if (exists) {
            throw new ConflictException("CUIT_ALREADY_EXISTS", "Ya existe una empresa con ese CUIT.");
        }
    }
}
