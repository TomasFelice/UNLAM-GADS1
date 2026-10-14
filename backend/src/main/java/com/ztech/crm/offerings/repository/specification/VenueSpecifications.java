package com.ztech.crm.offerings.repository.specification;

import com.ztech.crm.offerings.domain.Venue;
import com.ztech.crm.offerings.domain.enums.VenueStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class VenueSpecifications {
    private VenueSpecifications() { }
    public static Specification<Venue> tenant(Long id) {
        return (root, query, builder) -> builder.equal(root.get("tenantId"), id);
    }
    public static Specification<Venue> contains(String value) {
        String pattern = "%" + value.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("description")), pattern),
                builder.like(builder.lower(root.get("locality")), pattern));
    }
    public static Specification<Venue> status(VenueStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }
}
