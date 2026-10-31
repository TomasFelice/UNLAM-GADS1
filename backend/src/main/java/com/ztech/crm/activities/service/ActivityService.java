package com.ztech.crm.activities.service;

import com.ztech.crm.activities.dto.response.ActivityTypeSummaryResponse;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.activities.domain.Activity;
import com.ztech.crm.activities.dto.response.ActivityResponse;
import com.ztech.crm.activities.dto.response.ActivityAuthorResponse;
import com.ztech.crm.activities.dto.request.CreateActivityRequest;
import com.ztech.crm.shared.exception.BusinessException;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ztech.crm.activities.repository.ActivityRepository;
import com.ztech.crm.activities.repository.specification.ActivitySpecifications;
import com.ztech.crm.catalogs.service.ActivityTypeService;
import com.ztech.crm.customers.service.CompanyService;
import com.ztech.crm.customers.service.ContactService;
import com.ztech.crm.opportunities.service.OpportunityAccessService;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityRepository repository;
    private final ActivityTypeService activityTypeService;
    private final UserService userService;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final OpportunityAccessService opportunityAccessService;

    public ActivityService(ActivityRepository repository, ActivityTypeService activityTypeService,
                           UserService userService, CompanyService companyService,
                           ContactService contactService, OpportunityAccessService opportunityAccessService) {
        this.repository = repository;
        this.activityTypeService = activityTypeService;
        this.userService = userService;
        this.companyService = companyService;
        this.contactService = contactService;
        this.opportunityAccessService = opportunityAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(Pageable pageable) {
        Specification<Activity> specification = readable();
        return PageResponse.from(repository.findAll(specification, stable(pageable)), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byCompany(Long id, Pageable pageable) {
        companyService.assertExists(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.company(id)), stable(pageable)), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byContact(Long id, Pageable pageable) {
        contactService.assertExists(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.contact(id)), stable(pageable)), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byOpportunity(Long id, Pageable pageable) {
        opportunityAccessService.getReadableContext(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.opportunity(id)), stable(pageable)), this::map);
    }

    @Transactional(readOnly = true)
    public ActivitySummaryContext summaryContext(Long id) {
        opportunityAccessService.getReadableContext(id);
        var filter = ActivitySpecifications.tenant(TenantContext.currentTenantId())
                .and(ActivitySpecifications.opportunity(id));
        var page = repository.findAll(filter, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "occurredAt", "id")));
        return new ActivitySummaryContext(page.getTotalElements(), page.stream().map(a ->
                new ActivitySummaryContext.Fact(a.getId(), activityTypeService.getSummary(a.getTypeId()).name(),
                        a.getOccurredAt(), a.getCreatedAt(), a.getDescription(), a.getResult())).toList());
    }

    @Transactional
    public ActivityResponse create(CreateActivityRequest request) {
        Long company = request.companyId(); Long contact = request.contactId();
        if (request.opportunityId() != null) {
            var context = opportunityAccessService.getReadableContext(request.opportunityId());
            assertCompatible(company, context.companyId()); assertCompatible(contact, context.contactId());
            company = context.companyId(); contact = context.contactId();
        } else if (contact != null) {
            var context = contactService.getForActivity(contact);
            assertCompatible(company, context.companyId()); company = context.companyId();
        } else if (company != null) {
            companyService.getForActivity(company);
        } else {
            throw new BusinessException("ACTIVITY_WITHOUT_PARTY", "Elegí una empresa, un contacto o una oportunidad.");
        }
        activityTypeService.assertSelectable(request.typeId(), null);
        var current = TenantContext.currentUser();
        return map(repository.saveAndFlush(new Activity(current.tenantId(), request.typeId(), request.occurredAt(),
                company, contact, request.opportunityId(), request.description(), request.result(), current.userId())));
    }
    private void assertCompatible(Long requested, Long derived) {
        if (requested != null && !Objects.equals(requested, derived))
            throw new BusinessException("ACTIVITY_ASSOCIATION_MISMATCH", "Las asociaciones indicadas no coinciden con el cliente de la actividad.");
    }
    private Pageable stable(Pageable pageable) {
        var order = pageable.getSort();
        if (order.getOrderFor("id") == null) order = order.and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), order);
    }

    private Specification<Activity> readable() {
        var current = TenantContext.currentUser();
        Specification<Activity> specification = ActivitySpecifications.tenant(current.tenantId());
        if (current.role() == Role.SELLER) {
            specification = specification.and(ActivitySpecifications.visible(companyService.findReadableIds(),
                    contactService.findReadableIds(), opportunityAccessService.findReadableIds()));
        }
        return specification;
    }

    private ActivityResponse map(Activity activity) {
        var type = activityTypeService.getSummary(activity.getTypeId());
        var author = userService.getIdentity(activity.getCreatedBy());
        return new ActivityResponse(activity.getId(), new ActivityTypeSummaryResponse(type.id(), type.name(), type.active()),
                activity.getOccurredAt(), activity.getCompanyId(), activity.getContactId(),
                activity.getOpportunityId(), activity.getDescription(), activity.getResult(),
                new ActivityAuthorResponse(author.id(), author.firstName(), author.lastName()), activity.getCreatedAt());
    }
}
