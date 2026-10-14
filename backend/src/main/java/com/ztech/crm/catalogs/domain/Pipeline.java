package com.ztech.crm.catalogs.domain;

import com.ztech.crm.shared.audit.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipelines")
public class Pipeline extends TenantOwnedEntity {
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean active;
    @Column(name = "is_default", nullable = false) private boolean defaultPipeline;
    protected Pipeline() { }
    public Pipeline(Long tenantId, String name) { super(tenantId); this.name = name.strip(); }
    public void updateDetails(String name, boolean active) { this.name = name.strip(); this.active = active; }
    public void setDefaultPipeline(boolean value) { this.defaultPipeline = value; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public boolean isDefaultPipeline() { return defaultPipeline; }
}
