package com.ztech.crm.catalogs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loss_reasons")
public class LossReason {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, updatable = false) private Long tenantId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean active = true;
    protected LossReason() { }
    public LossReason(Long tenantId, String name, boolean active) {
        this.tenantId = tenantId;
        updateDetails(name, active);
    }
    public void updateDetails(String name, boolean active) {
        this.name = name.strip();
        this.active = active;
    }
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
