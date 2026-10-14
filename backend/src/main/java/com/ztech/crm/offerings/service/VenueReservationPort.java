package com.ztech.crm.offerings.service;

/** Consulta de reservas confirmadas sin depender del dominio ni repositorio consumidor. */
public interface VenueReservationPort {
    boolean hasReservationAboveCapacity(Long tenantId, Long venueId, int capacity);
}
