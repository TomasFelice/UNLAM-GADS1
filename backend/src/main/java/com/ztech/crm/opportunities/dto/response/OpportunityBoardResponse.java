package com.ztech.crm.opportunities.dto.response;

import java.util.List;

public record OpportunityBoardResponse(OpportunityPipelineResponse pipeline, List<StageColumn> columns) {
    public record StageColumn(Long stageId, String stageName, String kind, boolean active, List<OpportunityResponse> opportunities) { }
}
