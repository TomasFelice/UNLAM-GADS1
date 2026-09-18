package com.ztech.crm.access.repository.specification;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.domain.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> tenant(Long tenantId) {
        return (root, query, builder) -> builder.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<User> contains(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.strip().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("email")), pattern),
                builder.like(builder.lower(root.get("firstName")), pattern),
                builder.like(builder.lower(root.get("lastName")), pattern));
    }

    public static Specification<User> role(Role role) {
        return role == null ? null : (root, query, builder) -> builder.equal(root.get("role"), role);
    }

    public static Specification<User> active(Boolean active) {
        return active == null ? null : (root, query, builder) -> builder.equal(root.get("active"), active);
    }
}
