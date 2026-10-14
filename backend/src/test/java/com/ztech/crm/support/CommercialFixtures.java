package com.ztech.crm.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Datos exclusivos de prueba: dos organizaciones, tres roles y cliente compartido. */
public final class CommercialFixtures {
    public static final String PASSWORD = "FixtureTemporal123!";

    private CommercialFixtures() { }

    public record Account(long id, String email) { }
    public record Organization(long id, Account admin, Account manager, Account seller,
                               Account otherSeller, long companyId, long contactId,
                               long venueId, long stageId, long eventTypeId, long pipelineId,
                               long ownOpportunityId, long otherOpportunityId) { }
    public record Scenario(Organization first, Organization second) { }

    public static Scenario create(JdbcTemplate jdbc, PasswordEncoder encoder, String key) {
        String hash = encoder.encode(PASSWORD);
        return new Scenario(organization(jdbc, hash, key + ".a"), organization(jdbc, hash, key + ".b"));
    }

    private static Organization organization(JdbcTemplate jdbc, String hash, String key) {
        long tenant = jdbc.queryForObject("INSERT INTO tenants (name) VALUES (?) RETURNING id",
                Long.class, "Fixture " + key);
        long pipeline = jdbc.queryForObject("INSERT INTO pipelines (tenant_id, name, active, is_default) VALUES (?, 'Eventos corporativos', TRUE, TRUE) RETURNING id", Long.class, tenant);
        Account admin = account(jdbc, tenant, hash, key, "ADMIN", "admin");
        Account manager = account(jdbc, tenant, hash, key, "SALES_MANAGER", "manager");
        Account seller = account(jdbc, tenant, hash, key, "SELLER", "seller");
        Account other = account(jdbc, tenant, hash, key, "SELLER", "other");
        long company = jdbc.queryForObject("""
                INSERT INTO companies (tenant_id, legal_name, business_name, status, sales_rep_id)
                VALUES (?, ?, ?, 'POTENCIAL', ?) RETURNING id
                """, Long.class, tenant, "Compartida " + key, "Compartida " + key, other.id());
        long contact = jdbc.queryForObject("""
                INSERT INTO contacts (tenant_id, company_id, first_name, last_name, status, sales_rep_id)
                VALUES (?, ?, 'Contacto', ?, 'POTENCIAL', ?) RETURNING id
                """, Long.class, tenant, company, key, other.id());
        long venue = jdbc.queryForObject("""
                INSERT INTO venues (tenant_id, name, capacity, rate, status)
                VALUES (?, ?, 80, 100000, 'DISPONIBLE') RETURNING id
                """, Long.class, tenant, "Salón " + key);
        long stage = jdbc.queryForObject("""
                INSERT INTO stages (tenant_id, pipeline_id, name, position, kind)
                VALUES (?, ?, 'Consulta recibida', 1, 'OPEN') RETURNING id
                """, Long.class, tenant, pipeline);
        jdbc.update("INSERT INTO stages (tenant_id, pipeline_id, name, position, kind) VALUES (?, ?, 'Ganada', 2, 'WON')", tenant, pipeline);
        jdbc.update("INSERT INTO stages (tenant_id, pipeline_id, name, position, kind) VALUES (?, ?, 'Perdida', 3, 'LOST')", tenant, pipeline);
        long eventType = jdbc.queryForObject("""
                INSERT INTO event_types (tenant_id, name) VALUES (?, 'Evento corporativo') RETURNING id
                """, Long.class, tenant);
        long type = jdbc.queryForObject("INSERT INTO activity_types (tenant_id, name) VALUES (?, 'Llamada') RETURNING id",
                Long.class, tenant);
        jdbc.update("INSERT INTO origins (tenant_id, name) VALUES (?, 'Referido')", tenant);
        jdbc.update("INSERT INTO loss_reasons (tenant_id, name) VALUES (?, 'Presupuesto')", tenant);
        long ownOpportunity = opportunity(jdbc, tenant, company, contact, seller.id(), venue, stage, eventType, key + " propia");
        long otherOpportunity = opportunity(jdbc, tenant, company, contact, other.id(), venue, stage, eventType, key + " ajena");
        for (long opportunity : new long[] {ownOpportunity, otherOpportunity}) {
            long author = opportunity == ownOpportunity ? seller.id() : other.id();
            jdbc.update("""
                    INSERT INTO stage_history (tenant_id, opportunity_id, to_stage_id, changed_at, changed_by)
                    VALUES (?, ?, ?, '2026-10-01T12:00:00Z', ?)
                    """, tenant, opportunity, stage, author);
            jdbc.update("""
                    INSERT INTO activities (tenant_id, type_id, occurred_at, company_id, contact_id,
                                            opportunity_id, description, result, created_by)
                    VALUES (?, ?, '2026-10-01T12:00:00Z', ?, ?, ?, 'Llamada realizada', 'Interés confirmado', ?)
                    """, tenant, type, company, contact, opportunity, author);
        }
        return new Organization(tenant, admin, manager, seller, other, company, contact, venue, stage,
                eventType, pipeline, ownOpportunity, otherOpportunity);
    }

    private static Account account(JdbcTemplate jdbc, long tenant, String hash, String key,
                                   String role, String name) {
        String email = name + "." + key + "@fixtures.invalid";
        long id = jdbc.queryForObject("""
                INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active,
                                   must_change_password)
                VALUES (?, ?, ?, ?, 'Fixture', ?, TRUE, FALSE) RETURNING id
                """, Long.class, tenant, email, hash, name, role);
        return new Account(id, email);
    }

    private static long opportunity(JdbcTemplate jdbc, long tenant, long company, long contact,
                                    long seller, long venue, long stage, long eventType, String title) {
        return jdbc.queryForObject("""
                INSERT INTO opportunities (pipeline_id, tenant_id, title, company_id, contact_id, sales_rep_id,
                    venue_id, stage_id, event_type_id, status, event_start, event_end, attendee_count)
                VALUES ((SELECT pipeline_id FROM stages WHERE id = ?), ?, ?, ?, ?, ?, ?, ?, ?, 'ABIERTA', '2030-01-01T12:00:00Z', '2030-01-01T16:00:00Z', 20)
                RETURNING id
                """, Long.class, stage, tenant, title, company, contact, seller, venue, stage, eventType);
    }
}
