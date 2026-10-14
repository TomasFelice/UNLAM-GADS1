package com.ztech.crm.opportunities.dto.response;

import com.ztech.crm.catalogs.service.StageInfo;

public record OpportunityStageResponse(Long id, Long pipelineId, String name, Integer position, String kind, boolean active) {
    public static OpportunityStageResponse from(StageInfo stage) {
        return stage == null ? null : new OpportunityStageResponse(stage.id(), stage.pipelineId(), stage.name(), stage.position(), stage.kind(), stage.active());
    }
}
