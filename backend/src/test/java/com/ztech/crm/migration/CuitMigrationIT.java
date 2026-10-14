package com.ztech.crm.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CuitMigrationIT {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    private String url;
    private JdbcTemplate jdbc;

    @BeforeEach
    void isolatedDatabase() throws Exception {
        String database = "cuit_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/" + database);
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").target(target).load();
    }

    @Test
    void emptyInstallationAndRepeatedMigrationPreserveSeedAndReservations() {
        var latest = flyway("latest");
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(5);
        latest.validate();
        assertThat(latest.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM companies", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stage_history", Integer.class)).isEqualTo(25);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE contype = 'x' AND conrelid = 'opportunities'::regclass", Integer.class)).isEqualTo(1);
    }

    @Test
    void upgradesV1WithoutChangingIdsRelationsOrInventingCheckDigits() {
        flyway("1").migrate();
        Long legacyId = jdbc.queryForObject("SELECT id FROM companies WHERE cuit = '30712345678'", Long.class);
        Long blankId = jdbc.queryForObject("SELECT id FROM companies WHERE id <> ? ORDER BY id LIMIT 1", Long.class, legacyId);
        jdbc.update("UPDATE companies SET cuit = ' 30-71234567-8 ' WHERE id = ?", legacyId);
        jdbc.update("UPDATE companies SET cuit = ? WHERE id = ?", " \t\n ", blankId);
        Long otherTenant = jdbc.queryForObject("INSERT INTO tenants(name) VALUES ('Otro tenant') RETURNING id", Long.class);
        Long otherUser = jdbc.queryForObject("""
                INSERT INTO users(tenant_id,email,password_hash,first_name,last_name,role)
                VALUES (?, 'cuit@fixtures.invalid', 'test-only', 'Otro', 'Usuario', 'ADMIN') RETURNING id
                """, Long.class, otherTenant);
        Long otherCompany = jdbc.queryForObject("""
                INSERT INTO companies(tenant_id,legal_name,business_name,cuit,status,sales_rep_id)
                VALUES (?, 'Otra empresa', 'Otra empresa', '30-71234567-8', 'POTENCIAL', ?) RETURNING id
                """, Long.class, otherTenant, otherUser);
        var before = jdbc.queryForList("SELECT id, tenant_id, company_id, contact_id, stage_id FROM opportunities ORDER BY id");
        var history = jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id");
        var activities = jdbc.queryForList("SELECT * FROM activities ORDER BY id");
        flyway("latest").migrate();
        assertThat(jdbc.queryForObject("SELECT cuit FROM companies WHERE id = ?", String.class, legacyId)).isEqualTo("30712345678");
        assertThat(jdbc.queryForObject("SELECT cuit FROM companies WHERE id = ?", String.class, blankId)).isNull();
        assertThat(jdbc.queryForObject("SELECT cuit FROM companies WHERE id = ?", String.class, otherCompany)).isEqualTo("30712345678");
        assertThat(jdbc.queryForList("SELECT id, tenant_id, company_id, contact_id, stage_id FROM opportunities ORDER BY id")).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id")).isEqualTo(history);
        assertThat(jdbc.queryForList("SELECT * FROM activities ORDER BY id")).isEqualTo(activities);
    }

    @Test
    void collisionRollsBackWithoutMergingOrLosingAnyCompany() {
        flyway("1").migrate();
        Long legacyId = jdbc.queryForObject("SELECT id FROM companies WHERE cuit = '30712345678'", Long.class);
        Long duplicateId = jdbc.queryForObject("SELECT id FROM companies WHERE id <> ? ORDER BY id LIMIT 1", Long.class, legacyId);
        jdbc.update("UPDATE companies SET cuit = '30-71234567-8' WHERE id = ?", duplicateId);
        jdbc.update("UPDATE companies SET cuit = '   ' WHERE id = (SELECT min(id) FROM companies WHERE id NOT IN (?, ?))", legacyId, duplicateId);
        var before = jdbc.queryForList("SELECT * FROM companies ORDER BY id");
        assertThatThrownBy(() -> flyway("latest").migrate()).hasMessageContaining("CUIT equivalentes");
        assertThat(jdbc.queryForList("SELECT * FROM companies ORDER BY id")).isEqualTo(before);
        assertThat(flyway("1").info().current().getVersion().getVersion()).isEqualTo("1");
    }
}
