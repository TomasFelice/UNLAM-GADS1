package com.ztech.crm.opportunities.service;

import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.shared.exception.ConflictException;
import java.util.Objects;

final class OpportunityVersion {
    private OpportunityVersion() { }
    static void assertExpected(Opportunity opportunity, Long expected) {
        if (expected == null || !Objects.equals(opportunity.getVersion(), expected)) {
            throw new ConflictException("OPPORTUNITY_VERSION_CONFLICT",
                    "La oportunidad cambió desde que la abriste. Recargá los datos antes de volver a guardar.");
        }
    }
}
