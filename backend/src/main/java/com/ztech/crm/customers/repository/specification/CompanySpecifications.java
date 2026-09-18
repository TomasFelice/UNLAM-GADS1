package com.ztech.crm.customers.repository.specification;

import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class CompanySpecifications {

    private CompanySpecifications() {
    }

    public static Specification<Company> tenant(Long tenantId) {
        return (root, query, builder) -> builder.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Company> contains(String q) {
        String pattern = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("legalName")), pattern),
                builder.like(builder.lower(root.get("businessName")), pattern),
                builder.like(builder.lower(root.get("cuit")), pattern),
                builder.like(builder.lower(root.get("industry")), pattern),
                builder.like(builder.lower(root.get("locality")), pattern));
    }

    public static Specification<Company> status(PartyStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Company> origin(Long originId) {
        return (root, query, builder) -> builder.equal(root.get("originId"), originId);
    }

    public static Specification<Company> salesRep(Long salesRepId) {
        return (root, query, builder) -> builder.equal(root.get("salesRepId"), salesRepId);
    }

    public static Specification<Company> visibleToSeller(Long sellerId, Set<Long> relatedIds) {
        return (root, query, builder) -> relatedIds.isEmpty()
                ? builder.equal(root.get("salesRepId"), sellerId)
                : builder.or(builder.equal(root.get("salesRepId"), sellerId), root.get("id").in(relatedIds));
    }
}
