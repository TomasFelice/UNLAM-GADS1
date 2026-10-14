package com.ztech.crm.shared;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class ObservabilityIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl); registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void problemsAndLogsCorrelateWithoutBodiesCredentialsOrOtherTenantData() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "observability");
        var data = scenario.first();
        var admin = authorized(data.admin().email());
        var seller = authorized(data.seller().email());
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); super.append(event); }
        };
        appender.start(); root.addAppender(appender);
        try {
            var bad = exchange("/activities", HttpMethod.POST, admin, Map.of("description", "MARCADOR_PRIVADO", "result", "PRIVADO"));
            problem(bad, 400, "VALIDATION_ERROR"); assertThat(bad.getBody().get("errors")).asList().isNotEmpty();
            problem(exchange("/origins", HttpMethod.POST, seller, Map.of("name", "MARCADOR_PRIVADO", "active", true)), 403, "FORBIDDEN");
            problem(exchange("/opportunities/" + scenario.second().ownOpportunityId(), HttpMethod.GET, admin, null), 404, "NOT_FOUND");
            long lost = jdbc.queryForObject("SELECT id FROM stages WHERE tenant_id=? AND kind='LOST'", Long.class, data.id());
            problem(exchange("/opportunities/" + data.ownOpportunityId() + "/stage", HttpMethod.POST, admin, Map.of("stageId", lost, "version", 0)), 409, "STAGE_KIND_MISMATCH");
            long type = jdbc.queryForObject("SELECT id FROM activity_types WHERE tenant_id=?", Long.class, data.id());
            problem(exchange("/activities", HttpMethod.POST, admin, Map.of("typeId", type, "companyId", data.companyId(), "occurredAt", "2099-01-01T12:00:00Z", "description", "MARCADOR_PRIVADO", "result", "PRIVADO")), 400, "VALIDATION_ERROR");
            problem(exchange("/activities", HttpMethod.POST, admin, Map.of("typeId", type, "occurredAt", "2026-01-01T12:00:00Z", "description", "MARCADOR_PRIVADO", "result", "PRIVADO")), 422, "ACTIVITY_WITHOUT_PARTY");
            var missing = exchange("/opportunities", HttpMethod.GET, new HttpHeaders(), null);
            problem(missing, 401, "UNAUTHENTICATED");
            var events = appender.list.stream().filter(e -> bad.getHeaders().getFirst("X-Request-Id").equals(e.getMDCPropertyMap().get("requestId"))).toList();
            assertThat(events).isNotEmpty().allSatisfy(e -> {
                assertThat(e.getMDCPropertyMap()).containsEntry("userId", Long.toString(data.admin().id())).containsEntry("tenantId", Long.toString(data.id()));
                assertThat(e.getThrowableProxy()).isNull();
            });
            assertThat(appender.list.stream().filter(e -> missing.getHeaders().getFirst("X-Request-Id").equals(e.getMDCPropertyMap().get("requestId"))).toList())
                    .isNotEmpty().allSatisfy(e -> assertThat(e.getMDCPropertyMap()).doesNotContainKeys("userId", "tenantId"));
            assertThat(appender.list).anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("Solicitud autenticada", "status=400", "durationMs="));
            String messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(java.util.stream.Collectors.joining("\n"));
            assertThat(messages).doesNotContain("MARCADOR_PRIVADO", "PRIVADO", CommercialFixtures.PASSWORD,
                    admin.getFirst("Authorization"), data.admin().email(), "observability.b", "password_hash", "SELECT", "INSERT", "UPDATE", "stacktrace");
        } finally { root.detachAppender(appender); appender.stop(); }
    }

    private void problem(ResponseEntity<Map> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/problem+json");
        assertThat(response.getBody()).containsEntry("status", status).containsEntry("code", code);
        assertThat(response.getBody().get("detail")).isInstanceOf(String.class);
        assertThat(response.getBody().get("requestId")).isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
        assertThat(response.getHeaders().getFirst("X-Request-Id")).matches("[0-9a-f-]{36}");
        assertThat(response.getBody()).containsKey("errors").doesNotContainKeys("trace", "exception", "sql", "tenantId", "userId");
        assertThat(response.getBody().toString()).doesNotContain("observability.b", "password_hash", "MARCADOR_PRIVADO");
    }
    private ResponseEntity<Map> exchange(String path, HttpMethod method, HttpHeaders headers, Object body) {
        return rest.exchange("/api/v1" + path, method, new HttpEntity<>(body, headers), Map.class);
    }
    private HttpHeaders authorized(String email) {
        var login = rest.postForEntity("/api/v1/auth/login", new LoginRequest(email, CommercialFixtures.PASSWORD), LoginResponse.class);
        var headers = new HttpHeaders(); headers.setBearerAuth(login.getBody().accessToken()); return headers;
    }
}
