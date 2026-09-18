package com.ztech.crm.activities.dto.response;

import com.ztech.crm.access.dto.response.UserSummaryResponse;
import com.ztech.crm.catalogs.dto.response.ActivityTypeResponse;
import java.time.Instant;

public record ActivityResponse(
        Long id, ActivityTypeResponse type, Instant occurredAt, Long companyId, Long contactId,
        Long opportunityId, String description, String result, UserSummaryResponse author, Instant createdAt) { }
