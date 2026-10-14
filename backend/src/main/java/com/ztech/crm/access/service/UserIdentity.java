package com.ztech.crm.access.service;

/** Identidad consultable dentro del tenant, incluso para responsables inactivos. */
public record UserIdentity(Long id, String firstName, String lastName) {
    public String fullName() { return firstName + " " + lastName; }
}
