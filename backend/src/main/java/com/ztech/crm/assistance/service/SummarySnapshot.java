package com.ztech.crm.assistance.service;

import com.ztech.crm.assistance.dto.CommercialSummaryResponse.*;
import java.time.Instant;
import java.util.List;
public record SummarySnapshot(Long opportunityId, Long version, Instant contextAsOf, String fingerprint,
        String context, Coverage coverage, List<Source> sources) { }
