package com.ztech.crm.catalogs.repository.specification;

import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class CatalogSpecifications {
    private CatalogSpecifications() { }
    public static <T> Specification<T> filter(Long tenantId, String q, Boolean active) {
        return (root, query, builder) -> {
            var predicate = builder.equal(root.get("tenantId"), tenantId);
            if (q != null && !q.isBlank()) {
                String literal = q.strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("name")), "%" + literal + "%", '\\'));
            }
            if (active != null) predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            return predicate;
        };
    }
}
