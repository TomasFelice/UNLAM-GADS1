package com.ztech.crm.opportunities.repository.specification;

import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class OpportunitySpecifications {
    private OpportunitySpecifications() { }
    public static Specification<Opportunity> tenant(Long id) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), id);
    }
    public static Specification<Opportunity> contains(String q) {
        String pattern = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }
    public static Specification<Opportunity> status(OpportunityStatus value) { return equal("status", value); }
    public static Specification<Opportunity> stage(Long value) { return equal("stageId", value); }
    public static Specification<Opportunity> origin(Long value) { return equal("originId", value); }
    public static Specification<Opportunity> salesRep(Long value) { return equal("salesRepId", value); }
    public static Specification<Opportunity> venue(Long value) { return equal("venueId", value); }
    public static Specification<Opportunity> company(Long value) { return equal("companyId", value); }
    public static Specification<Opportunity> contact(Long value) { return equal("contactId", value); }
    public static Specification<Opportunity> eventFrom(Instant value) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventStart"), value);
    }
    public static Specification<Opportunity> eventTo(Instant value) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventEnd"), value);
    }
    private static <T> Specification<Opportunity> equal(String field, T value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }
}
