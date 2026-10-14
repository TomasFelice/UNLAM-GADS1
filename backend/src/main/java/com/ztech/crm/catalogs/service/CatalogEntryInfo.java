package com.ztech.crm.catalogs.service;

/** Consulta entre módulos, también para referencias históricas inactivas. */
public record CatalogEntryInfo(Long id, String name, boolean active) { }
