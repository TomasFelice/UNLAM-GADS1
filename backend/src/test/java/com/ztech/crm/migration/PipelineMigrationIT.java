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
class PipelineMigrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    private String url;
    private JdbcTemplate jdbc;
    @BeforeEach void isolated() throws Exception {
        String database = "pipelines_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/" + database);
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }
    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).locations("classpath:db/migration").target(target).load();
    }
    @Test void installsFromEmptyAndReexecutesWithoutChangingData() {
        assertThat(flyway("latest").migrate().migrationsExecuted).isEqualTo(5);
        var pipelines = jdbc.queryForList("SELECT * FROM pipelines ORDER BY id");
        var stages = jdbc.queryForList("SELECT * FROM stages ORDER BY id");
        assertThat(pipelines).hasSize(1);
        assertThat(stages).hasSize(7);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pipelines WHERE active AND is_default", Integer.class)).isEqualTo(1);
        assertThat(flyway("latest").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForList("SELECT * FROM pipelines ORDER BY id")).isEqualTo(pipelines);
        assertThat(jdbc.queryForList("SELECT * FROM stages ORDER BY id")).isEqualTo(stages);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname='no_overlapping_won_reservation'", Integer.class)).isEqualTo(1);
    }
    @Test void upgradesV1PreservingIdsHistoryAndIndependentTenantsAndCompositeKeys() {
        flyway("1").migrate();
        long second = jdbc.queryForObject("INSERT INTO tenants(name) VALUES ('Segundo proceso') RETURNING id", Long.class);
        jdbc.update("INSERT INTO stages(tenant_id,name,position,kind,active) SELECT ?,name,position,kind,active FROM stages WHERE tenant_id=(SELECT min(id) FROM tenants)", second);
        addLegacyCommercialData(second);
        long empty = jdbc.queryForObject("INSERT INTO tenants(name) VALUES ('Sin etapas previas') RETURNING id", Long.class);
        String opportunityColumns = "id,tenant_id,title,stage_id,company_id,contact_id,sales_rep_id,venue_id,event_type_id,status,event_start,event_end,attendee_count,final_value,closed_at,loss_reason_id,version";
        var opportunities = jdbc.queryForList("SELECT " + opportunityColumns + " FROM opportunities ORDER BY id");
        var services = jdbc.queryForList("SELECT * FROM opportunity_event_services ORDER BY opportunity_id,event_service_id");
        var stages = jdbc.queryForList("SELECT id,tenant_id,name,position,kind,active FROM stages ORDER BY id");
        var history = jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id");
        var activities = jdbc.queryForList("SELECT * FROM activities ORDER BY id");
        flyway("latest").migrate();
        assertThat(jdbc.queryForList("SELECT " + opportunityColumns + " FROM opportunities ORDER BY id")).isEqualTo(opportunities);
        assertThat(jdbc.queryForList("SELECT * FROM opportunity_event_services ORDER BY opportunity_id,event_service_id")).isEqualTo(services);
        assertThat(jdbc.queryForList("SELECT id,tenant_id,name,position,kind,active FROM stages WHERE tenant_id<>? ORDER BY id", empty)).isEqualTo(stages);
        assertThat(jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id")).isEqualTo(history);
        assertThat(jdbc.queryForList("SELECT * FROM activities ORDER BY id")).isEqualTo(activities);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT pipeline_id) FROM stages", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stages WHERE tenant_id=?", Integer.class, empty)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunities o JOIN stages s ON s.id=o.stage_id WHERE o.pipeline_id=s.pipeline_id AND o.tenant_id=s.tenant_id", Integer.class)).isEqualTo(opportunities.size());
        long foreignPipeline = jdbc.queryForObject("SELECT id FROM pipelines WHERE tenant_id=?", Long.class, second);
        assertThatThrownBy(() -> jdbc.update("UPDATE opportunities SET pipeline_id=? WHERE id=(SELECT min(id) FROM opportunities)", foreignPipeline)).hasMessageContaining("fk_opportunities_pipeline");
        assertThatThrownBy(() -> jdbc.update("UPDATE stages SET pipeline_id=? WHERE tenant_id=?", foreignPipeline, empty)).hasMessageContaining("fk_stages_pipeline");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO opportunities (tenant_id,pipeline_id,title,company_id,sales_rep_id,venue_id,
                    stage_id,event_type_id,status,event_start,event_end,attendee_count,final_value,closed_at)
                SELECT tenant_id,pipeline_id,'Reserva duplicada',company_id,sales_rep_id,venue_id,
                    stage_id,event_type_id,status,event_start,event_end,attendee_count,final_value,closed_at
                FROM opportunities WHERE tenant_id=? AND status='GANADA'
                """, second)).hasMessageContaining("no_overlapping_won_reservation");
        assertThat(flyway("latest").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForList("SELECT " + opportunityColumns + " FROM opportunities ORDER BY id")).isEqualTo(opportunities);
    }
    private void addLegacyCommercialData(long tenant) {
        long user = jdbc.queryForObject("""
                INSERT INTO users(tenant_id,email,password_hash,first_name,last_name,role)
                SELECT ?,'migration@fixtures.invalid',password_hash,'Migración','Segundo tenant','ADMIN'
                FROM users ORDER BY id LIMIT 1 RETURNING id
                """, Long.class, tenant);
        long company = jdbc.queryForObject("INSERT INTO companies(tenant_id,legal_name,business_name,status,sales_rep_id) VALUES (?,'Legacy SA','Legacy','INACTIVO',?) RETURNING id", Long.class, tenant, user);
        long contact = jdbc.queryForObject("INSERT INTO contacts(tenant_id,company_id,first_name,last_name,status,sales_rep_id) VALUES (?,?,'Ana','Legacy','NO_CONTACTAR',?) RETURNING id", Long.class, tenant, company, user);
        long venue = jdbc.queryForObject("INSERT INTO venues(tenant_id,name,capacity,rate,status) VALUES (?,'Legacy',80,100000,'DISPONIBLE') RETURNING id", Long.class, tenant);
        long event = jdbc.queryForObject("INSERT INTO event_types(tenant_id,name) VALUES (?,'Legacy') RETURNING id", Long.class, tenant);
        long activity = jdbc.queryForObject("INSERT INTO activity_types(tenant_id,name) VALUES (?,'Llamada legacy') RETURNING id", Long.class, tenant);
        long reason = jdbc.queryForObject("INSERT INTO loss_reasons(tenant_id,name) VALUES (?,'Presupuesto legacy') RETURNING id", Long.class, tenant);
        long service = jdbc.queryForObject("INSERT INTO event_services(tenant_id,name,price) VALUES (?,'Servicio legacy',100) RETURNING id", Long.class, tenant);
        for (String status : new String[] {"ABIERTA", "GANADA", "PERDIDA"}) {
            String kind = status.equals("GANADA") ? "WON" : status.equals("PERDIDA") ? "LOST" : "OPEN";
            long stage = jdbc.queryForObject("SELECT id FROM stages WHERE tenant_id=? AND kind=? ORDER BY position LIMIT 1", Long.class, tenant, kind);
            long opportunity = jdbc.queryForObject("""
                    INSERT INTO opportunities(tenant_id,title,company_id,contact_id,sales_rep_id,venue_id,
                        stage_id,event_type_id,status,event_start,event_end,attendee_count,final_value,closed_at,loss_reason_id)
                    VALUES (?,?,?,?,?,?,?,?,?,'2042-01-01T12:00:00Z','2042-01-01T16:00:00Z',20,?,?::timestamptz,?) RETURNING id
                    """, Long.class, tenant, "Legacy " + status, company, contact, user, venue, stage, event, status,
                    status.equals("GANADA") ? 120000 : null, status.equals("ABIERTA") ? null : "2026-01-01T12:00:00Z", status.equals("PERDIDA") ? reason : null);
            jdbc.update("INSERT INTO opportunity_event_services(opportunity_id,event_service_id) VALUES (?,?)", opportunity, service);
            jdbc.update("INSERT INTO stage_history(tenant_id,opportunity_id,to_stage_id,changed_at,changed_by,note) VALUES (?,?,?,'2026-01-01T12:00:00Z',?,'Evento legacy conservado')", tenant, opportunity, stage, user);
            jdbc.update("INSERT INTO activities(tenant_id,type_id,occurred_at,company_id,contact_id,opportunity_id,description,result,created_by) VALUES (?,?,'2026-01-01T12:00:00Z',?,?,?,'Hecho legacy','Conservado',?)", tenant, activity, company, contact, opportunity, user);
        }
    }
    @Test void incompleteLegacyConfigurationRollsBackWithoutInventingStagesOrLosingHistory() {
        flyway("1").migrate();
        jdbc.update("UPDATE stages SET active=false WHERE kind='LOST'");
        var stages = jdbc.queryForList("SELECT * FROM stages ORDER BY id");
        var history = jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id");
        assertThatThrownBy(() -> flyway("latest").migrate()).hasMessageContaining("embudo inicial incompleto");
        assertThat(jdbc.queryForList("SELECT * FROM stages ORDER BY id")).isEqualTo(stages);
        assertThat(jdbc.queryForList("SELECT id,tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_at,changed_by,note FROM stage_history ORDER BY id")).isEqualTo(history);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_name='pipelines'", Integer.class)).isZero();
    }
}
