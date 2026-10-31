package com.ztech.crm.offerings.service;

import java.math.BigDecimal;
public record EventServiceInfo(Long id, String name, BigDecimal price, boolean active) { }
