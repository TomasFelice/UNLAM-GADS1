package com.ztech.crm.catalogs.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderStagesRequest(@NotNull List<@NotNull Long> stageIds) { }
