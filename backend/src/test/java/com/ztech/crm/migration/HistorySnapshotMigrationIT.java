package com.ztech.crm.migration;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class HistorySnapshotMigrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @Test void preservesEveryLegacyFieldWithoutInventingPriorClosureSnapshots() throws Exception {
        String database = "history_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
                var statement = connection.createStatement()) { statement.execute("CREATE DATABASE " + database); }
        String url = postgres.getJdbcUrl().replace("/"+postgres.getDatabaseName(),"/"+database);
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(url,postgres.getUsername(),postgres.getPassword()));
        Flyway.configure().dataSource(url,postgres.getUsername(),postgres.getPassword()).target("1").load().migrate();
        String columns = "id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note";
        var before = jdbc.queryForList("SELECT " + columns + " FROM stage_history ORDER BY id");
        var closures = jdbc.queryForList("SELECT id,status,final_value,closed_at,loss_reason_id,version FROM opportunities ORDER BY id");
        var latest = Flyway.configure().dataSource(url,postgres.getUsername(),postgres.getPassword()).load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT " + columns + " FROM stage_history ORDER BY id")).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT id,status,final_value,closed_at,loss_reason_id,version FROM opportunities ORDER BY id")).isEqualTo(closures);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stage_history WHERE event_type='LEGACY' AND NOT snapshot_complete AND destination_status IS NULL AND final_value IS NULL AND closed_at IS NULL AND loss_reason_id IS NULL AND loss_reason_name IS NULL",Integer.class)).isEqualTo(before.size());
        assertThat(latest.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname='no_overlapping_won_reservation'",Integer.class)).isEqualTo(1);
    }
}
