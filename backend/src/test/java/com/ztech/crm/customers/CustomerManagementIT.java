package com.ztech.crm.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
class CustomerManagementIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test
    void savesAndFiltersOriginInheritsAndReassignsResponsibleWithAuthorizedNames() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "customer.assignment");
        var a = scenario.first();
        long origin = origin(a.id());
        var admin = login(a.admin());
        var manager = login(a.manager());
        var seller = login(a.seller());
        var company = company(a.seller().id(), origin);
        long companyId = id(send("companies", HttpMethod.POST, company, admin, 201));
        assertThat(get("companies/" + companyId, admin)).containsEntry("originId", (int) origin)
                .containsEntry("salesRepName", "seller Fixture");
        assertThat(get("companies?originId=" + origin, admin).get("totalElements")).isEqualTo(1);

        var contact = contact(companyId, null, origin);
        long contactId = id(send("contacts", HttpMethod.POST, contact, admin, 201));
        assertThat(get("contacts/" + contactId, admin)).containsEntry("salesRepId", (int) a.seller().id())
                .containsEntry("salesRepName", "seller Fixture");
        company.put("salesRepId", a.otherSeller().id());
        send("companies/" + companyId, HttpMethod.PUT, company, manager, 200);
        contact.put("salesRepId", a.otherSeller().id());
        send("contacts/" + contactId, HttpMethod.PUT, contact, manager, 200);
        send("companies/" + companyId, HttpMethod.GET, null, seller, 404);
        send("contacts/" + contactId, HttpMethod.GET, null, seller, 404);

        // El vendedor lee el cliente compartido sin ampliar /users/assignable.
        assertThat(get("companies/" + a.companyId(), seller)).containsEntry("salesRepName", "other Fixture");
        jdbc.update("UPDATE users SET active = FALSE WHERE id = ?", a.otherSeller().id());
        assertThat(get("contacts/" + a.contactId(), seller)).containsEntry("salesRepName", "other Fixture");
        assertThat(get("companies/" + companyId, admin)).containsEntry("salesRepName", "other Fixture");
    }

    @Test
    void rejectsForeignOrNewInactiveOriginsAndPreservesPreviousInactiveReference() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "customer.origin");
        var a = scenario.first();
        var admin = login(a.admin());
        long origin = origin(a.id());
        long foreign = origin(scenario.second().id());
        var company = company(a.seller().id(), origin);
        var contact = contact(a.companyId(), a.seller().id(), origin);
        long companyId = id(send("companies", HttpMethod.POST, company, admin, 201));
        long contactId = id(send("contacts", HttpMethod.POST, contact, admin, 201));
        for (String resource : new String[]{"companies", "contacts"}) {
            var payload = resource.equals("companies") ? company : contact;
            long id = resource.equals("companies") ? companyId : contactId;
            payload.put("originId", foreign);
            send(resource, HttpMethod.POST, payload, admin, 404);
            send(resource + "/" + id, HttpMethod.PUT, payload, admin, 404);
            payload.put("originId", origin);
        }
        jdbc.update("UPDATE origins SET active = FALSE WHERE id = ?", origin);
        send("companies", HttpMethod.POST, company, admin, 422);
        send("contacts", HttpMethod.POST, contact, admin, 422);
        company.put("notes", "Conservar origen histórico");
        contact.put("notes", "Conservar origen histórico");
        send("companies/" + companyId, HttpMethod.PUT, company, admin, 200);
        send("contacts/" + contactId, HttpMethod.PUT, contact, admin, 200);
        assertThat(get("companies/" + companyId, admin)).containsEntry("originId", (int) origin);
        assertThat(get("contacts/" + contactId, admin)).containsEntry("originId", (int) origin);
    }

    @Test
    void inactiveAndDoNotContactPreserveCommercialRelationsAndAuthorizedConsultation() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "customer.history");
        var a = scenario.first();
        var admin = login(a.admin());
        var seller = login(a.seller());
        long origin = origin(a.id());
        var company = company(a.otherSeller().id(), origin);
        var contact = contact(a.companyId(), a.otherSeller().id(), origin);
        var opportunitiesBefore = jdbc.queryForList("SELECT * FROM opportunities WHERE tenant_id = ? ORDER BY id", a.id());
        var activitiesBefore = jdbc.queryForList("SELECT * FROM activities WHERE tenant_id = ? ORDER BY id", a.id());
        var stagesBefore = jdbc.queryForList("SELECT * FROM stage_history WHERE tenant_id = ? ORDER BY id", a.id());
        for (String status : new String[]{"INACTIVO", "NO_CONTACTAR"}) {
            company.put("status", status);
            contact.put("status", status);
            send("companies/" + a.companyId(), HttpMethod.PUT, company, admin, 200);
            send("contacts/" + a.contactId(), HttpMethod.PUT, contact, admin, 200);
            for (var headers : new HttpHeaders[]{admin, seller}) {
                assertThat(get("companies/" + a.companyId(), headers)).containsEntry("status", status);
                assertThat(get("contacts/" + a.contactId(), headers)).containsEntry("status", status);
                var opportunity = get("opportunities/" + a.ownOpportunityId(), headers);
                assertThat(opportunity.get("company")).isInstanceOfSatisfying(Map.class,
                        value -> assertThat(value.get("id")).isEqualTo((int) a.companyId()));
                assertThat(opportunity.get("contact")).isInstanceOfSatisfying(Map.class,
                        value -> assertThat(value.get("id")).isEqualTo((int) a.contactId()));
                assertThat(get("opportunities/" + a.ownOpportunityId() + "/activities", headers).get("totalElements"))
                        .isEqualTo(1);
                var history = http.exchange("/api/v1/opportunities/" + a.ownOpportunityId() + "/stage-history",
                        HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {});
                assertThat(history.getStatusCode().value()).isEqualTo(200);
                assertThat(history.getBody()).hasSize(1);
            }
            assertThat(get("companies/" + a.companyId() + "/activities", admin).get("totalElements")).isEqualTo(2);
            assertThat(get("contacts/" + a.contactId() + "/activities", admin).get("totalElements")).isEqualTo(2);
            assertThat(jdbc.queryForList("SELECT * FROM opportunities WHERE tenant_id = ? ORDER BY id", a.id()))
                    .isEqualTo(opportunitiesBefore);
            assertThat(jdbc.queryForList("SELECT * FROM activities WHERE tenant_id = ? ORDER BY id", a.id()))
                    .isEqualTo(activitiesBefore);
            assertThat(jdbc.queryForList("SELECT * FROM stage_history WHERE tenant_id = ? ORDER BY id", a.id()))
                    .isEqualTo(stagesBefore);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM companies WHERE id = ?", Integer.class, a.companyId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM contacts WHERE id = ?", Integer.class, a.contactId())).isEqualTo(1);
        }
        var foreign = login(scenario.second().admin());
        send("companies/" + a.companyId() + "/activities", HttpMethod.GET, null, foreign, 404);
        send("contacts/" + a.contactId() + "/activities", HttpMethod.GET, null, foreign, 404);
    }

    private long origin(long tenant) {
        return jdbc.queryForObject("SELECT id FROM origins WHERE tenant_id = ?", Long.class, tenant);
    }
    private Map<String, Object> company(long salesRep, long origin) {
        return new HashMap<>(Map.of("legalName", "Cliente de prueba", "businessName", "Cliente de prueba",
                "status", "POTENCIAL", "salesRepId", salesRep, "originId", origin));
    }
    private Map<String, Object> contact(long company, Long salesRep, long origin) {
        var data = new HashMap<String, Object>(Map.of("firstName", "Persona", "lastName", "Prueba",
                "status", "POTENCIAL", "companyId", company, "originId", origin));
        if (salesRep != null) data.put("salesRepId", salesRep);
        return data;
    }
    private HttpHeaders login(CommercialFixtures.Account account) {
        var response = http.postForEntity("/api/v1/auth/login",
                new LoginRequest(account.email(), CommercialFixtures.PASSWORD), LoginResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
    private ResponseEntity<Map<String, Object>> send(String path, HttpMethod method, Object payload, HttpHeaders headers, int status) {
        var response = http.exchange("/api/v1/" + path, method, new HttpEntity<>(payload, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(response.getStatusCode().value()).as(method + " " + path + ": " + response.getBody()).isEqualTo(status);
        return response;
    }
    private Map<String, Object> get(String path, HttpHeaders headers) {
        return send(path, HttpMethod.GET, null, headers, 200).getBody();
    }
    private long id(ResponseEntity<Map<String, Object>> response) {
        return ((Number) response.getBody().get("id")).longValue();
    }
}
