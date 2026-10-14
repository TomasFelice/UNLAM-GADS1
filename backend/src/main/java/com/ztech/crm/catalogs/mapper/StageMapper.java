package com.ztech.crm.catalogs.mapper;

import com.ztech.crm.catalogs.domain.Stage;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import org.springframework.stereotype.Component;

@Component
public class StageMapper {

    public StageResponse toResponse(Stage stage) {
        return new StageResponse(stage.getId(), stage.getPipelineId(), stage.getName(), stage.getPosition(), stage.getKind(), stage.isActive());
    }
}
