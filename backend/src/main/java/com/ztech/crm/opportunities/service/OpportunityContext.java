package com.ztech.crm.opportunities.service;

/** Contexto autorizado para registrar hechos sin exponer la entidad fuera del módulo. */
public record OpportunityContext(Long id, Long companyId, Long contactId, Long salesRepId, String status) { }
