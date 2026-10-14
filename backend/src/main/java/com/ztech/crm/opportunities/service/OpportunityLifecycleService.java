package com.ztech.crm.opportunities.service;

import com.ztech.crm.access.service.UserService;
import com.ztech.crm.catalogs.service.LossReasonService;
import com.ztech.crm.catalogs.service.PipelineService;
import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.domain.StageHistory;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.opportunities.dto.request.*;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.opportunities.mapper.OpportunityMapper;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.security.TenantContext;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityLifecycleService {
    private final OpportunityAccessService access;
    private final OpportunityRepository opportunities;
    private final StageHistoryRepository history;
    private final StageService stages;
    private final PipelineService pipelines;
    private final UserService users;
    private final LossReasonService reasons;
    private final OpportunityAvailabilityService availability;
    private final OpportunityMapper mapper;

    public OpportunityLifecycleService(OpportunityAccessService access, OpportunityRepository opportunities,
            StageHistoryRepository history, StageService stages, PipelineService pipelines, UserService users,
            LossReasonService reasons, OpportunityAvailabilityService availability, OpportunityMapper mapper) {
        this.access = access; this.opportunities = opportunities; this.history = history; this.stages = stages;
        this.pipelines = pipelines; this.users = users; this.reasons = reasons;
        this.availability = availability; this.mapper = mapper;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    @Transactional
    public OpportunityResponse assign(Long id, AssignOpportunityRequest request) {
        var opportunity = open(id, request.version());
        users.assertActiveAndAssignable(request.salesRepId());
        opportunity.assign(request.salesRepId());
        return mapper.toResponse(opportunities.saveAndFlush(opportunity));
    }

    @Transactional
    public OpportunityResponse win(Long id, WinOpportunityRequest request) {
        var opportunity = open(id, request.version());
        stages.assertKindAndActive(request.stageId(), opportunity.getPipelineId(), "WON");
        availability.assertConfirmable(opportunity);
        Long from = opportunity.getStageId();
        opportunity.win(request.stageId(), request.finalValue(), Instant.now());
        return record(opportunity, from, request.note(), "WON", null);
    }

    @Transactional
    public OpportunityResponse lose(Long id, LoseOpportunityRequest request) {
        var opportunity = open(id, request.version());
        stages.assertKindAndActive(request.stageId(), opportunity.getPipelineId(), "LOST");
        reasons.assertSelectable(request.lossReasonId(), null);
        String reasonName = reasons.getSummary(request.lossReasonId()).name();
        Long from = opportunity.getStageId();
        opportunity.lose(request.stageId(), request.lossReasonId(), Instant.now());
        return record(opportunity, from, request.note(), "LOST", reasonName);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    @Transactional
    public OpportunityResponse reopen(Long id, ReopenOpportunityRequest request) {
        var opportunity = access.getReadableOrThrow(id);
        OpportunityVersion.assertExpected(opportunity, request.version());
        if (opportunity.getStatus() == OpportunityStatus.ABIERTA)
            throw new ConflictException("OPPORTUNITY_ALREADY_OPEN", "La oportunidad ya está abierta.");
        pipelines.assertActive(opportunity.getPipelineId());
        stages.assertKindAndActive(request.stageId(), opportunity.getPipelineId(), "OPEN");
        Long from = opportunity.getStageId();
        opportunity.reopen(request.stageId());
        return record(opportunity, from, request.note().strip(), "REOPENED", null);
    }

    private Opportunity open(Long id, Long version) {
        var opportunity = access.getReadableOrThrow(id);
        if (opportunity.getStatus() != OpportunityStatus.ABIERTA)
            throw new ConflictException("OPPORTUNITY_CLOSED", "La oportunidad está cerrada. Reabrila antes de modificarla.");
        OpportunityVersion.assertExpected(opportunity, version);
        return opportunity;
    }
    private OpportunityResponse record(Opportunity opportunity, Long from, String note, String event, String reason) {
        // Se fuerza el UPDATE antes del INSERT: GiST/versión fallan sin un historial parcial.
        opportunities.saveAndFlush(opportunity);
        history.saveAndFlush(StageHistory.lifecycle(opportunity, from, TenantContext.currentUser().userId(), note, event, reason));
        return mapper.toResponse(opportunity);
    }
}
