package com.ztech.crm.customers.domain;

import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.shared.audit.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Persona (BE-CUS-04..06). {@code company} es opcional — un contacto puede ser cliente
 * individual (Módulo 1 de la consigna). La relación con {@code Company} sí es un
 * {@code @ManyToOne} normal porque ambas entidades viven en el mismo módulo
 * ({@code customers}); no aplica la restricción de ADR-001 entre módulos distintos.
 */
@Entity
@Table(name = "contacts")
public class Contact extends TenantOwnedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String document;

    private String position;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyStatus status;

    @Column(name = "sales_rep_id", nullable = false)
    private Long salesRepId;

    @Column(name = "origin_id")
    private Long originId;

    private String notes;

    protected Contact() {
    }

    public Contact(Long tenantId, String firstName, String lastName, PartyStatus status) {
        super(tenantId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
    }

    public void updateDetails(Company company, String firstName, String lastName, String document,
                               String position, String email, String phone, PartyStatus status,
                               Long salesRepId, Long originId, String notes) {
        this.company = company;
        this.firstName = firstName;
        this.lastName = lastName;
        this.document = document;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.salesRepId = salesRepId;
        this.originId = originId;
        this.notes = notes;
    }

    public Company getCompany() {
        return company;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDocument() {
        return document;
    }

    public String getPosition() {
        return position;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public PartyStatus getStatus() {
        return status;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public Long getOriginId() {
        return originId;
    }

    public String getNotes() {
        return notes;
    }
}
