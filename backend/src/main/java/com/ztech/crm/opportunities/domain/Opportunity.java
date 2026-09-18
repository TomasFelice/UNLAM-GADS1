package com.ztech.crm.opportunities.domain;

import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.shared.audit.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BE-OPP-01..03. Todo FK a otro módulo (empresa, contacto, responsable, salón, etapa,
 * origen, motivo de pérdida) es un {@code Long} plano, nunca un {@code @ManyToOne}
 * cruzando módulos (ADR-001) — se resuelven llamando al `service` dueño de cada uno.
 *
 * <p>El cambio de etapa, la asignación de responsable y el cierre (ganada/perdida) no
 * son ediciones genéricas: tienen sus propios casos de uso (Fase 4/7), así que
 * {@code stageId}, {@code salesRepId} y {@code status} no se tocan desde
 * {@link #updateDetails}.
 */
@Entity
@Table(name = "opportunities")
public class Opportunity extends TenantOwnedEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "sales_rep_id", nullable = false)
    private Long salesRepId;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "event_type_id", nullable = false)
    private Long eventTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStatus status;

    @Column(name = "estimated_value")
    private BigDecimal estimatedValue;

    @Column(name = "final_value")
    private BigDecimal finalValue;

    private Integer probability;

    @Column(name = "event_start", nullable = false)
    private Instant eventStart;

    @Column(name = "event_end", nullable = false)
    private Instant eventEnd;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "opportunity_event_services",
            joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "event_service_id", nullable = false)
    private Set<Long> eventServiceIds = new LinkedHashSet<>();

    @Column(name = "attendee_count", nullable = false)
    private Integer attendeeCount;

    @Column(name = "estimated_close_date")
    private LocalDate estimatedCloseDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "origin_id")
    private Long originId;

    @Column(name = "loss_reason_id")
    private Long lossReasonId;

    private String notes;

    @Version
    private Long version;

    protected Opportunity() {
    }

    public Opportunity(Long tenantId, String title, Long companyId, Long contactId, Long salesRepId,
                        Long venueId, Long stageId, Long eventTypeId, Instant eventStart,
                        Instant eventEnd, Integer attendeeCount) {
        super(tenantId);
        this.title = title;
        this.companyId = companyId;
        this.contactId = contactId;
        this.salesRepId = salesRepId;
        this.venueId = venueId;
        this.stageId = stageId;
        this.eventTypeId = eventTypeId;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.attendeeCount = attendeeCount;
        this.status = OpportunityStatus.ABIERTA;
    }

    public Opportunity(Long tenantId, String title, Long companyId, Long contactId, Long salesRepId,
                       Long venueId, Long stageId, Instant eventDate, Integer attendeeCount) {
        this(tenantId, title, companyId, contactId, salesRepId, venueId, stageId, 1L,
                eventDate, eventDate.plusSeconds(86_400), attendeeCount);
    }

    /**
     * Cambio de etapa (Fase 4) — deliberadamente separado de {@link #updateDetails}:
     * no es una edición genérica, es un caso de uso propio con su propio historial
     * (docs/arquitectura/02-modulos-y-capas.md). {@code ChangeStageService} es quien
     * decide si el destino es válido antes de llamar acá.
     */
    public void changeStage(Long newStageId) {
        this.stageId = newStageId;
    }

    public void updateDetails(String title, Long companyId, Long contactId, Long venueId, Long eventTypeId,
                               BigDecimal estimatedValue, Integer probability, Instant eventStart,
                               Instant eventEnd, Integer attendeeCount, LocalDate estimatedCloseDate,
                               Long originId, String notes, Set<Long> eventServiceIds) {
        this.title = title;
        this.companyId = companyId;
        this.contactId = contactId;
        this.venueId = venueId;
        this.eventTypeId = eventTypeId;
        this.estimatedValue = estimatedValue;
        this.probability = probability;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.attendeeCount = attendeeCount;
        this.estimatedCloseDate = estimatedCloseDate;
        this.originId = originId;
        this.notes = notes;
        this.eventServiceIds.clear();
        if (eventServiceIds != null) {
            this.eventServiceIds.addAll(eventServiceIds);
        }
    }

    public String getTitle() {
        return title;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getContactId() {
        return contactId;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public Long getVenueId() {
        return venueId;
    }

    public Long getStageId() {
        return stageId;
    }

    public Long getEventTypeId() {
        return eventTypeId;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public BigDecimal getFinalValue() {
        return finalValue;
    }

    public Integer getProbability() {
        return probability;
    }

    public Instant getEventStart() {
        return eventStart;
    }

    public Instant getEventEnd() {
        return eventEnd;
    }

    public Set<Long> getEventServiceIds() {
        return Set.copyOf(eventServiceIds);
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public LocalDate getEstimatedCloseDate() {
        return estimatedCloseDate;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Long getOriginId() {
        return originId;
    }

    public Long getLossReasonId() {
        return lossReasonId;
    }

    public String getNotes() {
        return notes;
    }
}
