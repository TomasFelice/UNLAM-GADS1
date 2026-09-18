package com.ztech.crm.offerings.domain;

import com.ztech.crm.offerings.domain.enums.VenueStatus;
import com.ztech.crm.shared.audit.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Salón (ADR-004) — el "producto o servicio" de la consigna que se reserva. Sólo
 * lectura en E1 (BE-OFF-01): el ABM completo llega en la Fase 6 (BE-OFF-02).
 */
@Entity
@Table(name = "venues")
public class Venue extends TenantOwnedEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    private BigDecimal rate;

    private String address;

    private String locality;

    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "equipment", columnDefinition = "text[]", nullable = false)
    private List<String> equipment = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueStatus status = VenueStatus.DISPONIBLE;

    protected Venue() {
    }

    public Venue(Long tenantId, String name, Integer capacity) {
        super(tenantId);
        this.name = name;
        this.capacity = capacity;
    }

    public void updateDetails(String name, Integer capacity, BigDecimal rate, String address,
                              String locality, String description, List<String> equipment, VenueStatus status) {
        this.name = name;
        this.capacity = capacity;
        this.rate = rate;
        this.address = address;
        this.locality = locality;
        this.description = description;
        this.equipment = equipment == null ? new ArrayList<>() : new ArrayList<>(equipment);
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getAddress() {
        return address;
    }

    public String getLocality() {
        return locality;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEquipment() {
        return List.copyOf(equipment);
    }

    public VenueStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return status == VenueStatus.DISPONIBLE;
    }
}
