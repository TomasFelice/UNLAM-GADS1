package com.ztech.crm.opportunities.service;

import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import java.time.Instant;

public record OpportunityFilter(
        String q, OpportunityStatus status, Long stageId, Long originId, Long salesRepId,
        Long venueId, Long companyId, Long contactId, Instant eventFrom, Instant eventTo) { }
