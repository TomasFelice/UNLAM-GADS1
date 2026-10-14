package com.ztech.crm.offerings.service;

import java.math.BigDecimal;
import java.util.List;

/** Contrato de consulta de oferta para otros módulos, incluyendo referencias históricas. */
public record VenueInfo(Long id, String name, Integer capacity, BigDecimal rate, String address,
                        String locality, String description, List<String> equipment, String status) {}
