package com.ztech.crm.activities.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activities")
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, updatable = false) private Long tenantId;
    @Column(name = "type_id", nullable = false, updatable = false) private Long typeId;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @Column(name = "company_id", updatable = false) private Long companyId;
    @Column(name = "contact_id", updatable = false) private Long contactId;
    @Column(name = "opportunity_id", updatable = false) private Long opportunityId;
    @Column(updatable = false) private String description;
    @Column(updatable = false) private String result;
    @Column(name = "created_by", nullable = false, updatable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected Activity() { }
    public Activity(Long tenantId, Long typeId, Instant occurredAt, Long companyId, Long contactId,
            Long opportunityId, String description, String result, Long actor) {
        this.tenantId = tenantId; this.typeId = typeId; this.occurredAt = occurredAt;
        this.companyId = companyId; this.contactId = contactId; this.opportunityId = opportunityId;
        this.description = description.strip(); this.result = result.strip(); this.createdBy = actor;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getTypeId() { return typeId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getCompanyId() { return companyId; }
    public Long getContactId() { return contactId; }
    public Long getOpportunityId() { return opportunityId; }
    public String getDescription() { return description; }
    public String getResult() { return result; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
