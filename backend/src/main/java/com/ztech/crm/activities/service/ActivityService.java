package com.ztech.crm.activities.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.service.UserService;
import com.ztech.crm.activities.domain.Activity;
import com.ztech.crm.activities.dto.response.ActivityResponse;
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
        return PageResponse.from(repository.findAll(specification, pageable), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byCompany(Long id, Pageable pageable) {
        companyService.getSummary(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.company(id)), pageable), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byContact(Long id, Pageable pageable) {
        contactService.getSummary(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.contact(id)), pageable), this::map);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> byOpportunity(Long id, Pageable pageable) {
        opportunityAccessService.getReadableOrThrow(id);
        return PageResponse.from(repository.findAll(readable().and(ActivitySpecifications.opportunity(id)), pageable), this::map);
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
        return new ActivityResponse(activity.getId(), activityTypeService.getSummary(activity.getTypeId()),
                activity.getOccurredAt(), activity.getCompanyId(), activity.getContactId(),
                activity.getOpportunityId(), activity.getDescription(), activity.getResult(),
                userService.getSummary(activity.getCreatedBy()), activity.getCreatedAt());
    }
}
