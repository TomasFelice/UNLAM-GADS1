package com.ztech.crm.assistance.repository;

import com.ztech.crm.assistance.domain.CommercialSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface CommercialSummaryRepository extends JpaRepository<CommercialSummary, Long> {
    Optional<CommercialSummary> findByTenantIdAndOpportunityIdAndContextFingerprintAndPromptVersion(
            Long tenantId, Long opportunityId, String contextFingerprint, String promptVersion);

    @Modifying
    @Query(value = """
            INSERT INTO commercial_summaries (tenant_id, opportunity_id, context_fingerprint, prompt_version, response_json)
            VALUES (:tenantId, :opportunityId, :fingerprint, :promptVersion, :responseJson)
            ON CONFLICT (tenant_id, opportunity_id) DO UPDATE SET
                context_fingerprint = EXCLUDED.context_fingerprint,
                prompt_version = EXCLUDED.prompt_version, response_json = EXCLUDED.response_json
            """, nativeQuery = true)
    void upsert(Long tenantId, Long opportunityId, String fingerprint, String promptVersion, String responseJson);
}
