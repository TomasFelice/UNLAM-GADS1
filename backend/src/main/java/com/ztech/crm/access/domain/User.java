package com.ztech.crm.access.domain;

import com.ztech.crm.shared.audit.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Persona que puede iniciar sesión. {@code email} es único a nivel sistema, no por
 * tenant (ver nota en {@code V1__initial_schema.sql}).
 */
@Entity
@Table(name = "users")
public class User extends TenantOwnedEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    protected User() {
    }

    public User(Long tenantId, String email, String passwordHash, String firstName, String lastName, Role role) {
        super(tenantId);
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public void updateProfile(String email, String firstName, String lastName, Role role, boolean active) {
        boolean invalidatesAuthentication = this.role != role || this.active != active;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = active;
        if (invalidatesAuthentication) {
            authVersion++;
        }
    }

    public void setTemporaryPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = true;
        authVersion++;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
        authVersion++;
    }

    public void requirePasswordChange() {
        this.mustChangePassword = true;
    }
}
