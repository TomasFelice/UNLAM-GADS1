package com.ztech.crm.assistance.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "commercial_summaries")
public class CommercialSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "opportunity_id", nullable = false)
    private Long opportunityId;
    @Column(name = "context_fingerprint", nullable = false, length = 64)
    private String contextFingerprint;
    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;
    @Column(name = "response_json", nullable = false, columnDefinition = "text")
    private String responseJson;

    protected CommercialSummary() { }
    public String getResponseJson() { return responseJson; }
}
