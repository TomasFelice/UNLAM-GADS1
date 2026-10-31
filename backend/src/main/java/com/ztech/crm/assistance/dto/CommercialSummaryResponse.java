package com.ztech.crm.assistance.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CommercialSummaryResponse(Long opportunityId, Instant generatedAt, Instant contextAsOf,
        Long opportunityVersion, Coverage coverage, Sections sections, List<Source> sources) {
    public record Item(String text, List<String> sourceIds) { }
    public record Sections(List<Item> clientNeeds, List<Item> currentSituation,
            List<Item> agreementsAndObjections, List<Item> unconfirmedInformation) { }
    public record Source(String sourceId, String type, Long originalId, String title, Instant occurredAt,
            Instant recordedAt, Map<String,String> fragments, boolean textTruncated) { }
    public record Range(Instant from, Instant to) { }
    public record Coverage(long totalActivities, int includedActivities, long totalTransitions,
            int includedTransitions, Range activityRange, Range transitionRange, boolean partial,
            boolean textTruncated, int contextCharacters, String selectionRule) { }
}
