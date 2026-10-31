package com.ztech.crm.opportunities.service;

import com.ztech.crm.catalogs.service.*;
import com.ztech.crm.offerings.service.*;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunitySummaryReader {
    private final OpportunityAccessService access;
    private final StageHistoryRepository history;
    private final StageService stages;
    private final PipelineService pipelines;
    private final VenueService venues;
    private final EventServiceService services;
    private final EventTypeService eventTypes;
    private final LossReasonService lossReasons;
    public OpportunitySummaryReader(OpportunityAccessService access, StageHistoryRepository history,
            StageService stages, PipelineService pipelines, VenueService venues, EventServiceService services,
            EventTypeService eventTypes, LossReasonService lossReasons) {
        this.access=access; this.history=history; this.stages=stages; this.pipelines=pipelines;
        this.venues=venues; this.services=services; this.eventTypes=eventTypes; this.lossReasons=lossReasons;
    }
    @Transactional(readOnly = true)
    public OpportunitySummaryContext read(Long id) {
        var o=access.getReadableOrThrow(id);
        var tenant=TenantContext.currentTenantId();
        var stage=stages.getSummary(o.getStageId());
        var venue=venues.getInfo(o.getVenueId());
        var recent=history.findByTenantIdAndOpportunityId(tenant,id,PageRequest.of(0,50,
                Sort.by(Sort.Direction.DESC,"changedAt","id"))).stream()
                .map(h -> new OpportunitySummaryContext.Transition(h.getId(),h.getChangedAt(),h.getEventType(),
                        h.getFromStageId()==null?null:stages.getSummary(h.getFromStageId()).name(),
                        stages.getSummary(h.getToStageId()).name(),h.getDestinationStatus(),h.isSnapshotComplete(),
                        h.getFinalValue(),h.getClosedAt(),h.getLossReasonName(),h.getNote())).toList();
        return new OpportunitySummaryContext(o.getId(),o.getVersion(),o.getSalesRepId(),o.getTitle(),o.getNotes(),
                o.getStatus().name(),stage.name(),stage.kind(),pipelines.getInfo(o.getPipelineId()).name(),
                venue.name(),venue.capacity(),venue.rate(),o.getEventServiceIds().stream().sorted()
                        .map(s -> { var v=services.getInfo(s); return v.name()+" (precio de catálogo ARS: "+v.price()+")"; }).toList(),
                eventTypes.getSummary(o.getEventTypeId()).name(),o.getEventStart(),o.getEventEnd(),o.getAttendeeCount(),
                o.getEstimatedValue(),o.getFinalValue(),o.getProbability(),o.getEstimatedCloseDate(),o.getClosedAt(),
                o.getLossReasonId()==null?null:lossReasons.getSummary(o.getLossReasonId()).name(),
                history.countByTenantIdAndOpportunityId(tenant,id),recent);
    }
}
