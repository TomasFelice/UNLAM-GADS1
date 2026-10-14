package com.ztech.crm.catalogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.catalogs.service.ActivityTypeService;
import com.ztech.crm.catalogs.service.LossReasonService;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.support.CommercialFixtures;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class CatalogAdministrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ActivityTypeService activityTypes;
    @Autowired LossReasonService lossReasons;

    @ParameterizedTest
    @ValueSource(strings = {"origins", "loss-reasons", "activity-types", "event-types"})
    void managesCatalogWithTenantUniquenessLogicalDeletionAndValidation(String resource) {
        var scenario = CommercialFixtures.create(jdbc, encoder, "cat.crud." + resource);
        var a = scenario.first();
        var headers = login(a.admin());
        var created = send(resource, HttpMethod.POST, entry("  Entrada comercial  ", true), headers, 201);
        long id = id(created);
        assertThat(created.getHeaders().getLocation().toString()).endsWith("/api/v1/" + resource + "/" + id);
        assertThat(created.getBody()).containsEntry("name", "Entrada comercial").containsEntry("active", true);
        send(resource, HttpMethod.POST, entry(" Entrada comercial ", false), headers, 409);
        send(resource, HttpMethod.POST, entry("Entrada comercial", true), login(scenario.second().admin()), 201);
        long other = id(send(resource, HttpMethod.POST, entry("Otra entrada", true), headers, 201));
        send(resource + "/" + other, HttpMethod.PUT, entry("Entrada comercial", true), headers, 409);
        send(resource + "/" + id, HttpMethod.PUT, entry("Entrada renombrada", false), headers, 200);
        assertThat(send(resource + "/" + id, HttpMethod.GET, null, headers, 200).getBody())
                .containsEntry("name", "Entrada renombrada").containsEntry("active", false);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table(resource) + " WHERE id = ?", Integer.class, id)).isEqualTo(1);
        send(resource + "/" + id, HttpMethod.PUT, entry("Entrada renombrada", true), headers, 200);
        int before = jdbc.queryForObject("SELECT count(*) FROM " + table(resource) + " WHERE tenant_id = ?", Integer.class, a.id());
        for (var invalid : List.of(Map.of("name", " ", "active", true), Map.of("active", true), Map.of("name", "Sin estado"))) {
            send(resource, HttpMethod.POST, invalid, headers, 400);
            send(resource + "/" + id, HttpMethod.PUT, invalid, headers, 400);
        }
        send(resource + "/" + id, HttpMethod.DELETE, null, headers, 405);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table(resource) + " WHERE tenant_id = ?", Integer.class, a.id())).isEqualTo(before);
        assertThat(send(resource + "/" + id, HttpMethod.GET, null, headers, 200).getBody()).containsEntry("name", "Entrada renombrada");
    }

    @ParameterizedTest
    @ValueSource(strings = {"origins", "loss-reasons", "activity-types", "event-types"})
    void enforcesRolesAndTenantForListsIdsAndWrites(String resource) {
        var scenario = CommercialFixtures.create(jdbc, encoder, "cat.scope." + resource);
        var a = scenario.first();
        long local = id(send(resource, HttpMethod.POST, entry("Local", true), login(a.admin()), 201));
        long foreign = id(send(resource, HttpMethod.POST, entry("Ajena", true), login(scenario.second().admin()), 201));
        for (var account : new CommercialFixtures.Account[]{a.admin(), a.manager(), a.seller()}) {
            var headers = login(account);
            send(resource + "/" + local, HttpMethod.GET, null, headers, 200);
            send(resource + "/" + foreign, HttpMethod.GET, null, headers, 404);
            var list = (List<Map<String, Object>>) send(resource, HttpMethod.GET, null, headers, 200).getBody().get("content");
            assertThat(list).extracting(v -> ((Number) v.get("id")).longValue()).contains(local).doesNotContain(foreign);
            send(resource + "/" + foreign, HttpMethod.PUT, entry("Inválida", false), headers, account.equals(a.admin()) ? 404 : 403);
            if (!account.equals(a.admin())) {
                send(resource, HttpMethod.POST, entry("Inválida", true), headers, 403);
                send(resource + "/" + local, HttpMethod.PUT, entry("Inválida", false), headers, 403);
            }
        }
        assertThat(jdbc.queryForObject("SELECT name FROM " + table(resource) + " WHERE id = ?", String.class, foreign)).isEqualTo("Ajena");
        assertThat(jdbc.queryForObject("SELECT active FROM " + table(resource) + " WHERE id = ?", Boolean.class, local)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"origins", "loss-reasons", "activity-types", "event-types"})
    void pagesSearchesFiltersAndValidatesSort(String resource) {
        var a = CommercialFixtures.create(jdbc, encoder, "cat.pages." + resource).first();
        for (int i = 0; i < 25; i++) jdbc.update("INSERT INTO " + table(resource) + " (tenant_id, name, active) VALUES (?, ?, ?)",
                a.id(), i == 24 ? "ZZZ Singular 100%_" : "AAA Entrada " + i, i % 2 == 0);
        var headers = login(a.seller());
        var first = send(resource, HttpMethod.GET, null, headers, 200).getBody();
        assertThat(first).containsEntry("size", 20).containsEntry("totalPages", 2);
        assertThat((List<?>) first.get("content")).hasSize(20);
        var second = send(resource + "?page=1", HttpMethod.GET, null, headers, 200).getBody();
        assertThat((List<?>) second.get("content")).anySatisfy(v -> assertThat(v.toString()).contains("ZZZ Singular"));
        assertThat(send(resource + "?q=SINGULAR", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("totalElements", 1);
        assertThat(send(resource + "?q=100%_", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("totalElements", 1);
        assertThat(send(resource + "?active=false", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("totalElements", 12);
        assertThat(send(resource + "?size=100&sort=name,desc", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("size", 100);
        for (String invalid : new String[]{"size=101", "size=0", "page=-1", "sort=tenantId,asc", "sort=name,no", "active=DESCONOCIDO"})
            send(resource + "?" + invalid, HttpMethod.GET, null, headers, 400);
    }

    @Test
    void migrationSeedsAllMinimumActivityTypes() {
        var names = jdbc.queryForList("SELECT name FROM activity_types WHERE tenant_id = (SELECT id FROM tenants WHERE name = 'zTech Eventos Corporativos') AND active", String.class);
        assertThat(names).contains("Llamada", "Correo electrónico", "Mensaje", "Reunión presencial", "Reunión virtual",
                "Demostración", "Envío de propuesta", "Nota interna", "Otro");
    }

    @Test
    void opportunityConservesInactiveReferencesAndRejectsNewOrForeignOnes() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "cat.history");
        var a = scenario.first();
        var headers = login(a.admin());
        long origin = id(send("origins", HttpMethod.POST, entry("Histórico", true), headers, 201));
        jdbc.update("UPDATE opportunities SET origin_id = ? WHERE id = ?", origin, a.ownOpportunityId());
        send("origins/" + origin, HttpMethod.PUT, entry("Histórico renombrado", false), headers, 200);
        send("event-types/" + a.eventTypeId(), HttpMethod.PUT, entry("Evento histórico", false), headers, 200);
        var request = opportunity(a);
        request.put("originId", origin);
        request.put("notes", "Conserva referencias anteriores");
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, headers, 200);
        var detail = send("opportunities/" + a.ownOpportunityId(), HttpMethod.GET, null, headers, 200).getBody();
        assertThat(detail).containsEntry("originId", (int) origin).containsEntry("eventTypeId", (int) a.eventTypeId());
        request.put("stageId", a.stageId());
        request.put("salesRepId", a.seller().id());
        send("opportunities", HttpMethod.POST, request, headers, 422);
        long activeType = id(send("event-types", HttpMethod.POST, entry("Activo", true), headers, 201));
        request.put("eventTypeId", activeType);
        assertThat(send("opportunities", HttpMethod.POST, request, headers, 422).getBody()).containsEntry("code", "ORIGIN_INACTIVE");
        long inactiveType = id(send("event-types", HttpMethod.POST, entry("Otro inactivo", false), headers, 201));
        request.put("eventTypeId", inactiveType);
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, headers, 422);
        request.put("eventTypeId", scenario.second().eventTypeId());
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, headers, 404);
        request.put("eventTypeId", activeType);
        long foreignOrigin = jdbc.queryForObject("SELECT id FROM origins WHERE tenant_id = ?", Long.class, scenario.second().id());
        request.put("originId", foreignOrigin);
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, headers, 404);
        request.remove("eventTypeId");
        send("opportunities", HttpMethod.POST, request, headers, 400);
        assertThat(jdbc.queryForObject("SELECT event_type_id FROM opportunities WHERE id = ?", Long.class, a.ownOpportunityId())).isEqualTo(a.eventTypeId());
    }

    @Test
    void activityAndLossReasonSelectionRejectInactiveOrForeignButHistoricalReadsRemain() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "cat.selection");
        var a = scenario.first();
        long type = jdbc.queryForObject("SELECT id FROM activity_types WHERE tenant_id = ?", Long.class, a.id());
        long reason = jdbc.queryForObject("SELECT id FROM loss_reasons WHERE tenant_id = ?", Long.class, a.id());
        long foreignType = jdbc.queryForObject("SELECT id FROM activity_types WHERE tenant_id = ?", Long.class, scenario.second().id());
        long foreignReason = jdbc.queryForObject("SELECT id FROM loss_reasons WHERE tenant_id = ?", Long.class, scenario.second().id());
        var headers = login(a.admin());
        send("activity-types/" + type, HttpMethod.PUT, entry("Tipo histórico", false), headers, 200);
        send("loss-reasons/" + reason, HttpMethod.PUT, entry("Motivo histórico", false), headers, 200);
        // §8/§9 consumirán estas validaciones al implementar cierres y registro de actividades.
        var user = new AuthenticatedUser(a.admin().id(), a.id(), a.admin().email(), null, Role.ADMIN, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        try {
            assertThatThrownBy(() -> activityTypes.assertSelectable(type, null)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> lossReasons.assertSelectable(reason, null)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> activityTypes.assertSelectable(foreignType, null)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> lossReasons.assertSelectable(foreignReason, null)).isInstanceOf(NotFoundException.class);
            assertThat(activityTypes.getSummary(type).name()).isEqualTo("Tipo histórico");
            assertThat(lossReasons.getSummary(reason).name()).isEqualTo("Motivo histórico");
        } finally { SecurityContextHolder.clearContext(); }
        var activities = send("opportunities/" + a.ownOpportunityId() + "/activities", HttpMethod.GET, null, headers, 200).getBody();
        assertThat(((List<?>) activities.get("content")).toString()).contains("Tipo histórico", "active=false");
    }

    private Map<String, Object> opportunity(CommercialFixtures.Organization a) {
        return new HashMap<>(Map.of("title", "Prueba de catálogo", "companyId", a.companyId(), "contactId", a.contactId(),
                "venueId", a.venueId(), "eventTypeId", a.eventTypeId(), "eventStart", "2030-01-01T12:00:00Z", "eventEnd", "2030-01-01T16:00:00Z", "attendeeCount", 20));
    }
    private String table(String resource) { return resource.replace('-', '_'); }
    private Map<String, Object> entry(String name, boolean active) { return Map.of("name", name, "active", active); }
    private HttpHeaders login(CommercialFixtures.Account account) {
        var response = http.postForEntity("/api/v1/auth/login", new LoginRequest(account.email(), CommercialFixtures.PASSWORD), LoginResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var headers = new HttpHeaders(); headers.setBearerAuth(response.getBody().accessToken()); return headers;
    }
    private ResponseEntity<Map<String, Object>> send(String path, HttpMethod method, Object payload, HttpHeaders headers, int status) {
        if (method == HttpMethod.PUT && path.matches("opportunities/\\d+") && payload instanceof Map<?,?> source) {
            var current = http.exchange("/api/v1/" + path, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Map<String,Object>>(){});
            var versioned = new HashMap<String,Object>(); source.forEach((key,value)->versioned.put(key.toString(),value));
            versioned.put("version", current.getBody().getOrDefault("version", 0)); payload = versioned;
        }
        var response = http.exchange("/api/v1/" + path, method, new HttpEntity<>(payload, headers), new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(response.getStatusCode().value()).as(method + " " + path + ": " + response.getBody()).isEqualTo(status); return response;
    }
    private long id(ResponseEntity<Map<String, Object>> response) { return ((Number) response.getBody().get("id")).longValue(); }
}
