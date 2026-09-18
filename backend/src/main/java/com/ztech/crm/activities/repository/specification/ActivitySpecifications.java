package com.ztech.crm.activities.repository.specification;

import com.ztech.crm.activities.domain.Activity;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class ActivitySpecifications {
    private ActivitySpecifications() { }
    public static Specification<Activity> tenant(Long id) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), id);
    }
    public static Specification<Activity> company(Long id) { return equal("companyId", id); }
    public static Specification<Activity> contact(Long id) { return equal("contactId", id); }
    public static Specification<Activity> opportunity(Long id) { return equal("opportunityId", id); }
    public static Specification<Activity> visible(Set<Long> companyIds, Set<Long> contactIds,
                                                   Set<Long> opportunityIds) {
        return (root, query, cb) -> cb.or(
                companyIds.isEmpty() ? cb.disjunction() : root.get("companyId").in(companyIds),
                contactIds.isEmpty() ? cb.disjunction() : root.get("contactId").in(contactIds),
                opportunityIds.isEmpty() ? cb.disjunction() : root.get("opportunityId").in(opportunityIds));
    }
    private static Specification<Activity> equal(String field, Long id) {
        return (root, query, cb) -> cb.equal(root.get(field), id);
    }
}
