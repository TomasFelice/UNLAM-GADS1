package com.ztech.crm.opportunities.dto.response;

import com.ztech.crm.access.dto.response.UserSummaryResponse;
import java.time.Instant;

public record StageHistoryResponse(
        Long id, OpportunityStageResponse fromStage, OpportunityStageResponse toStage, Instant changedAt,
        UserSummaryResponse author, String note, String eventType, boolean snapshotComplete,
        String destinationStatus, java.math.BigDecimal finalValue, Instant closedAt,
        Long lossReasonId, String lossReasonName) { }
