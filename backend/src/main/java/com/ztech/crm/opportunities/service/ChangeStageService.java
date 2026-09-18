package com.ztech.crm.opportunities.service;

import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.domain.StageHistory;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.opportunities.dto.request.ChangeStageRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.opportunities.mapper.OpportunityMapper;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BE-OPP-05, design.md §9.1. Caso de uso propio, no una edición genérica: cambia
 * {@code Opportunity.stageId} y agrega un {@code StageHistory} en la misma
 * transacción — si el insert del historial falla, Spring revierte también el cambio
 * de etapa (no hay ningún {@code catch} acá que lo evite).
 *
 * <p>Sólo mueve entre etapas {@code OPEN} (pasos 1-4 de design.md §9.1). Los pasos 5-6
 * (destino {@code WON}/{@code LOST} delega en "ganar"/"perder") llegan en la Fase 7:
 * por ahora, pedir un destino cerrado devuelve el mismo error que cualquier etapa no
 * abierta ({@link StageService#assertOpenAndActive}).
 */
@Service
public class ChangeStageService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAccessService opportunityAccessService;
    private final StageHistoryRepository stageHistoryRepository;
    private final StageService stageService;
    private final OpportunityMapper opportunityMapper;

    public ChangeStageService(OpportunityRepository opportunityRepository,
                               OpportunityAccessService opportunityAccessService,
                               StageHistoryRepository stageHistoryRepository, StageService stageService,
                               OpportunityMapper opportunityMapper) {
        this.opportunityRepository = opportunityRepository;
        this.opportunityAccessService = opportunityAccessService;
        this.stageHistoryRepository = stageHistoryRepository;
        this.stageService = stageService;
        this.opportunityMapper = opportunityMapper;
    }

    @Transactional
    public OpportunityResponse changeStage(Long opportunityId, ChangeStageRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        Opportunity opportunity = opportunityAccessService.getReadableOrThrow(opportunityId);

        if (opportunity.getStatus() != OpportunityStatus.ABIERTA) {
            throw new ConflictException("OPPORTUNITY_CLOSED", "No se puede cambiar de etapa una oportunidad cerrada.");
        }

        Long fromStageId = opportunity.getStageId();
        Long toStageId = request.stageId();
        if (fromStageId.equals(toStageId)) {
            throw new BusinessException("SAME_STAGE", "La oportunidad ya está en esa etapa.");
        }

        stageService.assertOpenAndActive(toStageId);

        opportunity.changeStage(toStageId);
        opportunityRepository.save(opportunity);

        Long changedBy = TenantContext.currentUser().userId();
        StageHistory history = new StageHistory(tenantId, opportunityId, fromStageId, toStageId, changedBy,
                request.note());
        stageHistoryRepository.save(history);

        return opportunityMapper.toResponse(opportunity);
    }

}
