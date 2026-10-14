package com.ztech.crm.opportunities.service;

import com.ztech.crm.catalogs.service.StageUsagePort;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OpportunityStageUsage implements StageUsagePort {
    private final OpportunityRepository opportunities;
    private final StageHistoryRepository history;
    public OpportunityStageUsage(OpportunityRepository opportunities, StageHistoryRepository history) { this.opportunities = opportunities; this.history = history; }
    @Override @Transactional(readOnly = true)
    public boolean isUsed(Long tenantId, Long stageId) {
        return opportunities.existsByTenantIdAndStageId(tenantId, stageId)
                || history.existsByTenantIdAndFromStageIdOrTenantIdAndToStageId(tenantId, stageId, tenantId, stageId);
    }
}
