package com.ztech.crm.catalogs.dto.response;

import com.ztech.crm.catalogs.domain.enums.StageKind;

public record StageResponse(
        Long id,
        Long pipelineId,
        String name,
        Integer position,
        StageKind kind,
        boolean active
) {
}
