package com.ztech.crm.activities.dto.response;

import java.time.Instant;

public record ActivityResponse(
        Long id, ActivityTypeSummaryResponse type, Instant occurredAt, Long companyId, Long contactId,
        Long opportunityId, String description, String result, ActivityAuthorResponse author, Instant createdAt) { }
