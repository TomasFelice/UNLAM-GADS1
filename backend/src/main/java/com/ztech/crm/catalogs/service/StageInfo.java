package com.ztech.crm.catalogs.service;

public record StageInfo(Long id, Long pipelineId, String name, Integer position, String kind, boolean active) { }
