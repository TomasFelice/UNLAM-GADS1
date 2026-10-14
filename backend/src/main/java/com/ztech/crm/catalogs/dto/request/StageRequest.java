package com.ztech.crm.catalogs.dto.request;

import com.ztech.crm.catalogs.domain.enums.StageKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StageRequest(@NotNull Long pipelineId,
                           @NotBlank(message = "El nombre es obligatorio.") String name,
                           @NotNull StageKind kind, @NotNull Boolean active) {
    public StageRequest { name = name == null ? null : name.strip(); }
}
