package com.ztech.crm.catalogs.dto.response;

import java.time.Instant;

public record PipelineResponse(Long id, String name, boolean active, boolean defaultPipeline,
                               Instant createdAt, Instant updatedAt) { }
