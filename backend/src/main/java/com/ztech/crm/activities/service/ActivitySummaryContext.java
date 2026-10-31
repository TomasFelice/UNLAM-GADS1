package com.ztech.crm.activities.service;

import java.time.Instant;
import java.util.List;
public record ActivitySummaryContext(long total, List<Fact> facts) {
    public record Fact(Long id, String type, Instant occurredAt, Instant createdAt, String description, String result) { }
}
