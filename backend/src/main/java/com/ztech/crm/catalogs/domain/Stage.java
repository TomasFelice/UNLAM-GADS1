package com.ztech.crm.catalogs.domain;

import com.ztech.crm.catalogs.domain.enums.StageKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Etapa del embudo (DP-06), configurable — no un enum fijo, porque la entrega final
 * exige un embudo configurable. No extiende {@code TenantOwnedEntity}: la tabla no
 * tiene auditoría completa (sólo {@code created_at}), a diferencia de las entidades de
 * negocio con historial propio.
 */
@Entity
@Table(name = "stages")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private Long pipelineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageKind kind;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Stage() {
    }

    public Stage(Long tenantId, Long pipelineId, String name, Integer position, StageKind kind) {
        this.tenantId = tenantId;
        this.pipelineId = pipelineId;
        this.name = name;
        this.position = position;
        this.kind = kind;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getPipelineId() { return pipelineId; }
    public void updateDetails(String name, StageKind kind, boolean active) {
        this.name = name.strip(); this.kind = kind; this.active = active;
    }
    public void reorder(Integer position) { this.position = position; }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public StageKind getKind() {
        return kind;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
