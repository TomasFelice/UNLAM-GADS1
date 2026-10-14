package com.ztech.crm.catalogs.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePipelineRequest(@NotBlank(message = "El nombre es obligatorio.") String name, String templateKey) {
    public CreatePipelineRequest { name = name == null ? null : name.strip(); }
}
