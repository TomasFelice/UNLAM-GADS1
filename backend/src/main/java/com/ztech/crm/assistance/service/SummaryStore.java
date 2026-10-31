package com.ztech.crm.assistance.service;

import com.ztech.crm.assistance.dto.CommercialSummaryResponse;
import com.ztech.crm.assistance.repository.CommercialSummaryRepository;
import com.ztech.crm.shared.security.TenantContext;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Persistencia breve; el coordinador autoriza mediante el snapshot antes de usarla. */
@Service
public class SummaryStore {
    private final CommercialSummaryRepository repository;
    private final JsonMapper json;

    public SummaryStore(CommercialSummaryRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Optional<CommercialSummaryResponse> find(SummarySnapshot snapshot) {
        return repository.findByTenantIdAndOpportunityIdAndContextFingerprintAndPromptVersion(
                TenantContext.currentTenantId(), snapshot.opportunityId(), snapshot.fingerprint(), SummaryPrompt.VERSION)
                .map(saved -> json.readValue(saved.getResponseJson(), CommercialSummaryResponse.class));
    }

    @Transactional(timeout = 5)
    public void save(SummarySnapshot snapshot, CommercialSummaryResponse response) {
        repository.upsert(TenantContext.currentTenantId(), snapshot.opportunityId(), snapshot.fingerprint(),
                SummaryPrompt.VERSION, json.writeValueAsString(response));
    }
}
