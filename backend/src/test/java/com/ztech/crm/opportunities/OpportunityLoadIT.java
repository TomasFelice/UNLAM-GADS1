package com.ztech.crm.opportunities;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import com.ztech.crm.support.LoadBaselineApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.ztech.crm.support.LoadBaselineApplication$SqlCounter")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(LoadBaselineApplication.BaselineConfiguration.class)
@Testcontainers
class OpportunityLoadIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl); registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void batchesDistinctReferencesAndCollectionsWithConstantSqlAndTenantSellerScope() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "load");
        var data = scenario.first();
        // Referencias distintas por fila: la caché JPA de un mismo ID no puede ocultar N+1.
        for (int index = 0; index < 98; index++) {
            long owner = index % 2 == 0 ? data.seller().id() : data.otherSeller().id();
            long company = jdbc.queryForObject("INSERT INTO companies (tenant_id, legal_name, business_name, status, sales_rep_id) VALUES (?, ?, ?, 'POTENCIAL', ?) RETURNING id", Long.class, data.id(), "Legal " + index, "Empresa lote " + index, owner);
            long contact = jdbc.queryForObject("INSERT INTO contacts (tenant_id, company_id, first_name, last_name, status, sales_rep_id) VALUES (?, ?, 'Contacto', ?, 'POTENCIAL', ?) RETURNING id", Long.class, data.id(), company, "Lote " + index, owner);
            long venue = jdbc.queryForObject("INSERT INTO venues (tenant_id, name, capacity, rate, status) VALUES (?, ?, 80, 100, 'DISPONIBLE') RETURNING id", Long.class, data.id(), "Salón lote " + index);
            long stage = jdbc.queryForObject("INSERT INTO stages (tenant_id, pipeline_id, name, position, kind) VALUES (?, ?, ?, ?, 'OPEN') RETURNING id", Long.class, data.id(), data.pipelineId(), "Etapa lote " + index, index + 10);
            long opportunity = jdbc.queryForObject("INSERT INTO opportunities (tenant_id, pipeline_id, title, company_id, contact_id, sales_rep_id, venue_id, stage_id, event_type_id, status, event_start, event_end, attendee_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ABIERTA', '2030-01-01T12:00:00Z', '2030-01-01T16:00:00Z', 10) RETURNING id", Long.class, data.id(), data.pipelineId(), "Oportunidad lote " + index, company, contact, owner, venue, stage, data.eventTypeId());
            long service = jdbc.queryForObject("INSERT INTO event_services (tenant_id, name, price) VALUES (?, ?, 10) RETURNING id", Long.class, data.id(), "Servicio lote " + index);
            jdbc.update("INSERT INTO opportunity_event_services (opportunity_id, event_service_id) VALUES (?, ?)", opportunity, service);
        }
        var admin = authorized(data.admin().email());
        var twenty = get("/opportunities?size=20", admin);
        var hundred = get("/opportunities?size=100", admin);
        assertThat(sql(hundred)).isLessThanOrEqualTo(sql(twenty)).isLessThanOrEqualTo(12);
        assertThat(hundred.getBody()).contains("\"totalElements\":100", "Empresa lote 97", "Contacto Lote 97", "Salón lote 97", "Etapa lote 97", "serviceIds").doesNotContain("load.b");
        var board = get("/opportunities/board?pipelineId=" + data.pipelineId(), admin);
        assertThat(sql(board)).isLessThanOrEqualTo(14);
        var seller = authorized(data.seller().email());
        var sellerTwenty = get("/opportunities?size=20", seller);
        var sellerHundred = get("/opportunities?size=100", seller);
        assertThat(sql(sellerHundred)).isLessThanOrEqualTo(sql(sellerTwenty)).isLessThanOrEqualTo(16);
        assertThat(sellerHundred.getBody()).contains("\"totalElements\":50", "Empresa lote 96").doesNotContain("Empresa lote 97", "load.a ajena", "load.b");
        assertThat(get("/opportunities?companyId=" + scenario.second().companyId(), admin).getBody()).contains("\"totalElements\":0");
        assertThat(rest.exchange("/api/v1/opportunities/" + scenario.second().ownOpportunityId(), HttpMethod.GET, admin, String.class).getStatusCode().value()).isEqualTo(404);
        System.out.printf("OpportunityLoadIT SQL admin 20=%d 100=%d board=%d seller 20=%d 100=%d%n", sql(twenty), sql(hundred), sql(board), sql(sellerTwenty), sql(sellerHundred));
    }

    private int sql(ResponseEntity<String> result) { return Integer.parseInt(result.getHeaders().getFirst("X-Baseline-Sql-Count")); }
    private ResponseEntity<String> get(String path, HttpEntity<Void> headers) {
        var response = rest.exchange("/api/v1" + path, HttpMethod.GET, headers, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200); return response;
    }
    private HttpEntity<Void> authorized(String email) {
        var login = rest.postForEntity("/api/v1/auth/login", new LoginRequest(email, CommercialFixtures.PASSWORD), LoginResponse.class);
        var headers = new HttpHeaders(); headers.setBearerAuth(login.getBody().accessToken()); return new HttpEntity<>(headers);
    }
}
