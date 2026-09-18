package com.ztech.crm.opportunities.dto.response;

import com.ztech.crm.access.dto.response.UserSummaryResponse;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import java.time.Instant;

public record StageHistoryResponse(
        Long id, StageResponse fromStage, StageResponse toStage, Instant changedAt,
        UserSummaryResponse author, String note) { }
