package com.ztech.crm.opportunities.service;

import com.ztech.crm.access.service.UserService;
import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.opportunities.dto.response.StageHistoryResponse;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageHistoryService {
    private final StageHistoryRepository repository;
    private final OpportunityAccessService opportunityAccessService;
    private final StageService stageService;
    private final UserService userService;
    public StageHistoryService(StageHistoryRepository repository, OpportunityAccessService opportunityAccessService,
                               StageService stageService, UserService userService) {
        this.repository = repository;
        this.opportunityAccessService = opportunityAccessService;
        this.stageService = stageService;
        this.userService = userService;
    }
    @Transactional(readOnly = true)
    public List<StageHistoryResponse> list(Long opportunityId) {
        opportunityAccessService.getReadableOrThrow(opportunityId);
        return repository.findAllByTenantIdAndOpportunityIdOrderByChangedAtAsc(
                TenantContext.currentTenantId(), opportunityId).stream()
                .map(value -> new StageHistoryResponse(value.getId(), value.getFromStageId() == null ? null
                        : stageService.getSummary(value.getFromStageId()), stageService.getSummary(value.getToStageId()),
                        value.getChangedAt(), userService.getSummary(value.getChangedBy()), value.getNote()))
                .toList();
    }
}
