package com.ztech.crm.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Comprueba la instalación consolidada y la protección de reservas desde una base vacía. */
@Testcontainers
class MigrationV1IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void installsSchemaAndDemoInOneMigrationAndProtectsReservations() throws Exception {
        var flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            try (var seed = statement.executeQuery("""
                    SELECT (SELECT COUNT(*) FROM users WHERE must_change_password) AS users,
                           (SELECT COUNT(*) FROM companies) AS companies,
                           (SELECT COUNT(*) FROM contacts) AS contacts,
                           (SELECT COUNT(*) FROM opportunities) AS opportunities,
                           (SELECT COUNT(*) FROM opportunity_event_services) AS services,
                           (SELECT COUNT(*) FROM stage_history) AS history,
                           (SELECT COUNT(*) FROM activities) AS activities
                    """)) {
                assertThat(seed.next()).isTrue();
                assertThat(seed.getInt("users")).isEqualTo(3);
                assertThat(seed.getInt("companies")).isEqualTo(5);
                assertThat(seed.getInt("contacts")).isEqualTo(6);
                assertThat(seed.getInt("opportunities")).isEqualTo(7);
                assertThat(seed.getInt("services")).isEqualTo(6);
                assertThat(seed.getInt("history")).isEqualTo(25);
                assertThat(seed.getInt("activities")).isEqualTo(10);
            }

            // Duplicar la reserva ganada del seed debe violar la exclusión GiST.
            String duplicateReservation = """
                    INSERT INTO opportunities (
                        tenant_id, title, company_id, sales_rep_id, venue_id, stage_id,
                        event_type_id, status, event_start, event_end, attendee_count)
                    SELECT tenant_id, 'Reserva de prueba', company_id, sales_rep_id, venue_id,
                           stage_id, event_type_id, status, %s, %s, attendee_count
                    FROM opportunities WHERE title = 'Directorio trimestral Río'
                    """;
            assertThatThrownBy(() -> statement.executeUpdate(
                    duplicateReservation.formatted("event_start", "event_end")))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo("23P01");

            // Los intervalos [inicio, fin) permiten una reserva contigua.
            assertThat(statement.executeUpdate(duplicateReservation.formatted(
                    "event_end", "event_end + INTERVAL '1 hour'"))).isEqualTo(1);

            assertThatThrownBy(() -> statement.executeUpdate(
                    duplicateReservation.formatted("event_end", "event_start")))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo("23514");
        }
    }
}
