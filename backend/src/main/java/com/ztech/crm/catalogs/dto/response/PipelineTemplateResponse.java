package com.ztech.crm.catalogs.dto.response;

import java.util.List;

public record PipelineTemplateResponse(String key, String name, List<TemplateStage> stages) {
    public record TemplateStage(String name, String kind) { }
}
