package com.ztech.crm.opportunities.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Cambio de etapa de una oportunidad (BE-ACT-03/04). Append-only por construcción, no
 * sólo por convención: no tiene setters — una vez creado, nada en esta clase permite
 * mutarlo. {@link com.ztech.crm.opportunities.repository.StageHistoryRepository}
 * bloquea además el borrado.
 */
@Entity
@Table(name = "stage_history")
public class StageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "opportunity_id", nullable = false, updatable = false)
    private Long opportunityId;

    @Column(name = "from_stage_id", updatable = false)
    private Long fromStageId;

    @Column(name = "to_stage_id", nullable = false, updatable = false)
    private Long toStageId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private Long changedBy;

    @Column(updatable = false)
    private String note;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;
    @Column(name = "snapshot_complete", nullable = false, updatable = false)
    private boolean snapshotComplete;
    @Column(name = "destination_status", updatable = false)
    private String destinationStatus;
    @Column(name = "final_value", updatable = false)
    private java.math.BigDecimal finalValue;
    @Column(name = "closed_at", updatable = false)
    private Instant closedAt;
    @Column(name = "loss_reason_id", updatable = false)
    private Long lossReasonId;
    @Column(name = "loss_reason_name", updatable = false)
    private String lossReasonName;

    protected StageHistory() {
    }

    public StageHistory(Long tenantId, Long opportunityId, Long fromStageId, Long toStageId, Long changedBy,
                         String note) {
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.fromStageId = fromStageId;
        this.toStageId = toStageId;
        this.changedBy = changedBy;
        this.note = note;
        this.eventType = fromStageId == null ? "CREATED" : "STAGE_CHANGED";
        this.snapshotComplete = true;
        this.destinationStatus = "ABIERTA";
    }

    /** Snapshot append-only: el motivo conserva el nombre que tenía al cerrar. */
    public static StageHistory lifecycle(Opportunity opportunity, Long fromStageId, Long actor,
            String note, String eventType, String lossReasonName) {
        var history = new StageHistory(opportunity.getTenantId(), opportunity.getId(), fromStageId,
                opportunity.getStageId(), actor, note);
        history.eventType = eventType;
        history.destinationStatus = opportunity.getStatus().name();
        history.finalValue = opportunity.getFinalValue();
        history.closedAt = opportunity.getClosedAt();
        history.lossReasonId = opportunity.getLossReasonId();
        history.lossReasonName = lossReasonName;
        return history;
    }

    public String getEventType() { return eventType; }
    public boolean isSnapshotComplete() { return snapshotComplete; }
    public String getDestinationStatus() { return destinationStatus; }
    public java.math.BigDecimal getFinalValue() { return finalValue; }
    public Instant getClosedAt() { return closedAt; }
    public Long getLossReasonId() { return lossReasonId; }
    public String getLossReasonName() { return lossReasonName; }

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public Long getFromStageId() {
        return fromStageId;
    }

    public Long getToStageId() {
        return toStageId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public String getNote() {
        return note;
    }
}
