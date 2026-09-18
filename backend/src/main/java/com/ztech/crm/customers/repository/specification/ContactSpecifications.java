package com.ztech.crm.customers.repository.specification;

import com.ztech.crm.customers.domain.Contact;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class ContactSpecifications {

    private ContactSpecifications() {
    }

    public static Specification<Contact> tenant(Long tenantId) {
        return (root, query, builder) -> builder.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Contact> contains(String q) {
        String pattern = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("firstName")), pattern),
                builder.like(builder.lower(root.get("lastName")), pattern),
                builder.like(builder.lower(root.get("email")), pattern),
                builder.like(builder.lower(root.get("document")), pattern),
                builder.like(builder.lower(root.get("company").get("businessName")), pattern));
    }

    public static Specification<Contact> status(PartyStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Contact> origin(Long originId) {
        return (root, query, builder) -> builder.equal(root.get("originId"), originId);
    }

    public static Specification<Contact> salesRep(Long salesRepId) {
        return (root, query, builder) -> builder.equal(root.get("salesRepId"), salesRepId);
    }

    public static Specification<Contact> company(Long companyId) {
        return (root, query, builder) -> builder.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Contact> visibleToSeller(Long sellerId, Set<Long> relatedIds) {
        return (root, query, builder) -> relatedIds.isEmpty()
                ? builder.equal(root.get("salesRepId"), sellerId)
                : builder.or(builder.equal(root.get("salesRepId"), sellerId), root.get("id").in(relatedIds));
    }
}
