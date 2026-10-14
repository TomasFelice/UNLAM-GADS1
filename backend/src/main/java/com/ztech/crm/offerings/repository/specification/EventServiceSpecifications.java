package com.ztech.crm.offerings.repository.specification;

import com.ztech.crm.offerings.domain.EventService;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EventServiceSpecifications {
    private EventServiceSpecifications() { }
    public static Specification<EventService> tenant(Long id) {
        return (root, query, builder) -> builder.equal(root.get("tenantId"), id);
    }
    public static Specification<EventService> contains(String value) {
        String pattern = "%" + value.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("description")), pattern));
    }
    public static Specification<EventService> active(boolean active) {
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }
}
