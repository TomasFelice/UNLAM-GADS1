package com.ztech.crm.opportunities.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.catalogs.service.EventTypeService;
import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.customers.dto.response.CompanySummaryResponse;
import com.ztech.crm.customers.dto.response.ContactSummaryResponse;
import com.ztech.crm.customers.service.CompanyService;
import com.ztech.crm.customers.service.ContactService;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.offerings.service.EventServiceService;
import com.ztech.crm.offerings.service.VenueService;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.opportunities.dto.request.CreateOpportunityRequest;
import com.ztech.crm.opportunities.dto.request.UpdateOpportunityRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityBoardResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityDetailResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.opportunities.mapper.OpportunityMapper;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.opportunities.domain.StageHistory;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.ForbiddenException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.TenantContext;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAccessService opportunityAccessService;
    private final OpportunityMapper opportunityMapper;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final VenueService venueService;
    private final StageService stageService;
    private final EventTypeService eventTypeService;
    private final EventServiceService eventServiceService;
    private final UserService userService;
    private final StageHistoryRepository stageHistoryRepository;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              OpportunityAccessService opportunityAccessService, OpportunityMapper opportunityMapper,
                              CompanyService companyService, ContactService contactService,
                              VenueService venueService, StageService stageService,
                              EventTypeService eventTypeService, EventServiceService eventServiceService,
                              UserService userService, StageHistoryRepository stageHistoryRepository) {
        this.opportunityRepository = opportunityRepository;
        this.opportunityAccessService = opportunityAccessService;
        this.opportunityMapper = opportunityMapper;
        this.companyService = companyService;
        this.contactService = contactService;
        this.venueService = venueService;
        this.stageService = stageService;
        this.eventTypeService = eventTypeService;
        this.eventServiceService = eventServiceService;
        this.userService = userService;
        this.stageHistoryRepository = stageHistoryRepository;
    }

    @Transactional
    public OpportunityResponse create(CreateOpportunityRequest request) {
        PartyResolution parties = resolveParties(request.companyId(), request.contactId());
        VenueResponse venue = venueService.getActiveOrThrow(request.venueId());
        stageService.assertOpenAndActive(request.stageId());
        eventTypeService.getActiveOrThrow(request.eventTypeId());
        assertEventRange(request.eventStart(), request.eventEnd());
        assertCapacity(venue, request.attendeeCount());
        Set<Long> serviceIds = validateServices(request.serviceIds());
        Long salesRepId = resolveSalesRep(request.salesRepId(), parties);

        Opportunity opportunity = new Opportunity(TenantContext.currentTenantId(), request.title(),
                parties.companyId(), request.contactId(), salesRepId, request.venueId(), request.stageId(),
                request.eventTypeId(), request.eventStart(), request.eventEnd(), request.attendeeCount());
        opportunity.updateDetails(request.title(), parties.companyId(), request.contactId(), request.venueId(),
                request.eventTypeId(), request.estimatedValue(), request.probability(), request.eventStart(),
                request.eventEnd(), request.attendeeCount(), request.estimatedCloseDate(), request.originId(),
                request.notes(), serviceIds);
        Opportunity saved = opportunityRepository.save(opportunity);
        stageHistoryRepository.save(new StageHistory(TenantContext.currentTenantId(), saved.getId(), null,
                saved.getStageId(), TenantContext.currentUser().userId(), "Etapa inicial"));
        return toEnrichedResponse(saved);
    }

    @Transactional
    public OpportunityResponse update(Long id, UpdateOpportunityRequest request) {
        Opportunity opportunity = opportunityAccessService.getReadableOrThrow(id);
        assertOpenOrThrow(opportunity);
        PartyResolution parties = resolveParties(request.companyId(), request.contactId());
        VenueResponse venue = venueService.getActiveOrThrow(request.venueId());
        eventTypeService.getActiveOrThrow(request.eventTypeId());
        assertEventRange(request.eventStart(), request.eventEnd());
        assertCapacity(venue, request.attendeeCount());
        Set<Long> serviceIds = validateServices(request.serviceIds());

        opportunity.updateDetails(request.title(), parties.companyId(), request.contactId(), request.venueId(),
                request.eventTypeId(), request.estimatedValue(), request.probability(), request.eventStart(),
                request.eventEnd(), request.attendeeCount(), request.estimatedCloseDate(), request.originId(),
                request.notes(), serviceIds);
        return toEnrichedResponse(opportunityRepository.save(opportunity));
    }

    @Transactional(readOnly = true)
    public OpportunityDetailResponse getDetail(Long id) {
        Opportunity opportunity = opportunityAccessService.getReadableOrThrow(id);
        CompanySummaryResponse company = opportunity.getCompanyId() == null
                ? null : companyService.getSummary(opportunity.getCompanyId());
        ContactSummaryResponse contact = opportunity.getContactId() == null
                ? null : contactService.getSummary(opportunity.getContactId());
        VenueResponse venue = venueService.getDetail(opportunity.getVenueId());
        StageResponse stage = stageService.getSummary(opportunity.getStageId());
        return opportunityMapper.toDetailResponse(opportunity, company, contact, venue, stage);
    }

    @Transactional(readOnly = true)
    public PageResponse<OpportunityResponse> list(OpportunityFilter filter, Pageable pageable) {
        Page<Opportunity> page = opportunityAccessService.findReadable(filter, pageable);
        return PageResponse.from(page, this::toEnrichedResponse);
    }

    @Transactional(readOnly = true)
    public OpportunityBoardResponse getBoard(OpportunityFilter filter) {
        Map<Long, List<OpportunityResponse>> byStage = opportunityAccessService.findAllReadable(filter).stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.groupingBy(OpportunityResponse::stageId));
        List<OpportunityBoardResponse.StageColumn> columns = stageService.list().stream()
                .map(stage -> new OpportunityBoardResponse.StageColumn(
                        stage.id(), stage.name(), byStage.getOrDefault(stage.id(), List.of())))
                .toList();
        return new OpportunityBoardResponse(columns);
    }

    private OpportunityResponse toEnrichedResponse(Opportunity opportunity) {
        String companyName = opportunity.getCompanyId() == null ? null
                : companyService.getSummary(opportunity.getCompanyId()).businessName();
        String contactName = null;
        if (opportunity.getContactId() != null) {
            ContactSummaryResponse contact = contactService.getSummary(opportunity.getContactId());
            contactName = contact.firstName() + " " + contact.lastName();
        }
        var user = userService.getSummary(opportunity.getSalesRepId());
        String salesRepName = user.firstName() + " " + user.lastName();
        String venueName = venueService.getDetail(opportunity.getVenueId()).name();
        String stageName = stageService.getSummary(opportunity.getStageId()).name();
        return opportunityMapper.toResponse(opportunity, companyName, contactName, salesRepName, venueName, stageName);
    }

    private PartyResolution resolveParties(Long requestedCompanyId, Long contactId) {
        if (requestedCompanyId == null && contactId == null) {
            throw new BusinessException("OPPORTUNITY_WITHOUT_PARTY",
                    "La oportunidad debe estar asociada a una empresa o a un contacto.");
        }
        ContactSummaryResponse contact = contactId == null ? null : contactService.getSummary(contactId);
        Long derivedCompanyId = contact == null ? null : contact.companyId();
        if (requestedCompanyId != null && derivedCompanyId != null && !requestedCompanyId.equals(derivedCompanyId)) {
            throw new BusinessException("CONTACT_COMPANY_MISMATCH",
                    "El contacto seleccionado pertenece a otra empresa.");
        }
        Long companyId = derivedCompanyId == null ? requestedCompanyId : derivedCompanyId;
        CompanySummaryResponse company = companyId == null ? null : companyService.getSummary(companyId);
        return new PartyResolution(companyId, company, contact);
    }

    private Long resolveSalesRep(Long requestedId, PartyResolution parties) {
        AuthenticatedUser current = TenantContext.currentUser();
        Long inherited = parties.contact() != null ? parties.contact().salesRepId()
                : parties.company() != null ? parties.company().salesRepId() : current.userId();
        Long resolved = requestedId == null ? inherited : requestedId;
        if (current.role() == Role.SELLER && !current.userId().equals(resolved)) {
            throw new ForbiddenException("SELLER_ASSIGNMENT_FORBIDDEN",
                    "Un vendedor sólo puede asignarse oportunidades a sí mismo.");
        }
        userService.assertActiveAndAssignable(resolved);
        return resolved;
    }

    private Set<Long> validateServices(Set<Long> serviceIds) {
        Set<Long> uniqueIds = serviceIds == null ? Set.of() : new LinkedHashSet<>(serviceIds);
        uniqueIds.forEach(eventServiceService::getActiveOrThrow);
        return uniqueIds;
    }

    private void assertEventRange(Instant eventStart, Instant eventEnd) {
        if (!eventEnd.isAfter(eventStart)) {
            throw new BusinessException("INVALID_EVENT_RANGE", "El fin del evento debe ser posterior al inicio.");
        }
    }

    private void assertCapacity(VenueResponse venue, Integer attendeeCount) {
        if (attendeeCount > venue.capacity()) {
            throw new BusinessException("VENUE_CAPACITY_EXCEEDED",
                    "La cantidad de asistentes supera la capacidad del salón.");
        }
    }

    private void assertOpenOrThrow(Opportunity opportunity) {
        if (opportunity.getStatus() != OpportunityStatus.ABIERTA) {
            throw new ConflictException("OPPORTUNITY_CLOSED", "No se puede modificar una oportunidad cerrada.");
        }
    }

    private record PartyResolution(Long companyId, CompanySummaryResponse company, ContactSummaryResponse contact) {
    }
}
