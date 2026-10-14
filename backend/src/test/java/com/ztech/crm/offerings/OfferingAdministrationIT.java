package com.ztech.crm.offerings;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import com.ztech.crm.shared.dto.PageResponse;
import java.util.HashMap;
import java.util.List;
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
class OfferingAdministrationIT {
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
    void adminCreatesEditsAndDeactivatesWithoutDeletingCommercialRelations() {
        var a = CommercialFixtures.create(jdbc, encoder, "offer.crud").first();
        var admin = login(a.admin());
        var venue = venue();
        venue.put("equipment", List.of(" Proyector ", "Audio"));
        venue.put("description", "  ");
        var createdVenue = send("venues", HttpMethod.POST, venue, admin, 201);
        long venueId = id(createdVenue);
        assertThat(createdVenue.getHeaders().getLocation().toString()).endsWith("/api/v1/venues/" + venueId);
        assertThat(createdVenue.getBody()).containsEntry("name", "Salón de prueba")
                .containsEntry("equipment", List.of("Proyector", "Audio")).containsEntry("description", null);
        var service = service();
        var createdService = send("event-services", HttpMethod.POST, service, admin, 201);
        long serviceId = id(createdService);
        assertThat(createdService.getHeaders().getLocation().toString()).endsWith("/api/v1/event-services/" + serviceId);
        jdbc.update("UPDATE opportunities SET venue_id = ? WHERE id = ?", venueId, a.ownOpportunityId());
        jdbc.update("INSERT INTO opportunity_event_services (opportunity_id, event_service_id) VALUES (?, ?)", a.ownOpportunityId(), serviceId);
        venue.put("status", "MANTENIMIENTO");
        send("venues/" + venueId, HttpMethod.PUT, venue, admin, 200);
        venue.put("status", "INACTIVO");
        venue.put("name", "Salón conservado");
        send("venues/" + venueId, HttpMethod.PUT, venue, admin, 200);
        service.put("active", false);
        service.put("name", "Servicio conservado");
        send("event-services/" + serviceId, HttpMethod.PUT, service, admin, 200);
        for (var account : new CommercialFixtures.Account[]{a.admin(), a.manager(), a.seller()}) {
            var headers = login(account);
            assertThat(send("venues/" + venueId, HttpMethod.GET, null, headers, 200).getBody())
                    .containsEntry("name", "Salón conservado").containsEntry("status", "INACTIVO");
            assertThat(send("event-services/" + serviceId, HttpMethod.GET, null, headers, 200).getBody())
                    .containsEntry("name", "Servicio conservado").containsEntry("active", false);
        }
        assertThat(jdbc.queryForObject("SELECT venue_id FROM opportunities WHERE id = ?", Long.class, a.ownOpportunityId())).isEqualTo(venueId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunity_event_services WHERE opportunity_id = ? AND event_service_id = ?",
                Integer.class, a.ownOpportunityId(), serviceId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT created_by FROM venues WHERE id = ?", Long.class, venueId)).isEqualTo(a.admin().id());
        assertThat(jdbc.queryForObject("SELECT updated_by FROM event_services WHERE id = ?", Long.class, serviceId)).isEqualTo(a.admin().id());
    }

    @Test
    void opportunityRetainsInactiveOfferingsButCannotAddNewOnes() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "offer.history");
        var a = scenario.first();
        var admin = login(a.admin());
        long historic = id(send("event-services", HttpMethod.POST, service(), admin, 201));
        long newInactive = id(send("event-services", HttpMethod.POST, service(), admin, 201));
        jdbc.update("INSERT INTO opportunity_event_services (opportunity_id, event_service_id) VALUES (?, ?)", a.ownOpportunityId(), historic);
        jdbc.update("UPDATE venues SET status = 'INACTIVO' WHERE id = ?", a.venueId());
        jdbc.update("UPDATE event_services SET active = false WHERE id IN (?, ?)", historic, newInactive);
        var request = opportunity(a);
        request.put("serviceIds", List.of(historic));
        request.put("notes", "Observaciones editadas con oferta histórica");
        for (var account : new CommercialFixtures.Account[]{a.admin(), a.manager(), a.seller()}) {
            send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, login(account), 200);
        }
        var detail = send("opportunities/" + a.ownOpportunityId(), HttpMethod.GET, null, admin, 200).getBody();
        assertThat(((Map<?, ?>) detail.get("venue")).get("status")).isEqualTo("INACTIVO");
        assertThat(detail).containsEntry("serviceIds", List.of((int) historic)).containsEntry("notes", request.get("notes"));
        // Omitir la colección conserva asociaciones; una lista vacía las elimina explícitamente.
        request.remove("serviceIds");
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunity_event_services WHERE opportunity_id = ?", Integer.class, a.ownOpportunityId())).isEqualTo(1);
        request.put("stageId", a.stageId());
        request.put("salesRepId", a.seller().id());
        send("opportunities", HttpMethod.POST, request, admin, 422);
        request.put("venueId", scenario.second().venueId());
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 404);
        long otherInactive = id(send("venues", HttpMethod.POST, venue(), admin, 201));
        jdbc.update("UPDATE venues SET status = 'MANTENIMIENTO' WHERE id = ?", otherInactive);
        request.put("venueId", otherInactive);
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 422);
        request.put("venueId", a.venueId());
        request.put("serviceIds", List.of(historic, newInactive));
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 422);
        request.put("serviceIds", List.of());
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 200);
        request.put("serviceIds", List.of(historic));
        send("opportunities/" + a.ownOpportunityId(), HttpMethod.PUT, request, admin, 422);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunity_event_services WHERE opportunity_id = ?", Integer.class, a.ownOpportunityId())).isZero();
    }

    @Test
    void capacityReductionProtectsConfirmedReservationsAndRevalidatesOpenOnEdit() {
        var a = CommercialFixtures.create(jdbc, encoder, "offer.capacity").first();
        var admin = login(a.admin());
        long wonStage = jdbc.queryForObject("SELECT id FROM stages WHERE tenant_id = ? AND kind = 'WON'", Long.class, a.id());
        jdbc.update("UPDATE opportunities SET status = 'GANADA', stage_id = ?, attendee_count = 30, closed_at = now(), final_value = 100 WHERE id = ?", wonStage, a.ownOpportunityId());
        var request = venue();
        request.put("capacity", 29);
        var conflict = send("venues/" + a.venueId(), HttpMethod.PUT, request, admin, 409);
        assertThat(conflict.getBody()).containsEntry("code", "VENUE_RESERVED_CAPACITY");
        assertThat(jdbc.queryForObject("SELECT capacity FROM venues WHERE id = ?", Integer.class, a.venueId())).isEqualTo(80);
        request.put("capacity", 30);
        send("venues/" + a.venueId(), HttpMethod.PUT, request, admin, 200);
        assertThat(jdbc.queryForObject("SELECT attendee_count FROM opportunities WHERE id = ?", Integer.class, a.ownOpportunityId())).isEqualTo(30);
        // Una oportunidad abierta no impide reducir; se vuelve a validar al editar.
        long openVenue = id(send("venues", HttpMethod.POST, venue(), admin, 201));
        jdbc.update("UPDATE opportunities SET venue_id = ? WHERE id = ?", openVenue, a.otherOpportunityId());
        request.put("capacity", 10);
        send("venues/" + openVenue, HttpMethod.PUT, request, admin, 200);
        var open = opportunity(a);
        open.put("venueId", openVenue);
        assertThat(send("opportunities/" + a.otherOpportunityId(), HttpMethod.PUT, open, admin, 422).getBody())
                .containsEntry("code", "VENUE_CAPACITY_EXCEEDED");
        assertThat(jdbc.queryForObject("SELECT attendee_count FROM opportunities WHERE id = ?", Integer.class, a.otherOpportunityId())).isEqualTo(20);
        open.put("attendeeCount", 10);
        send("opportunities/" + a.otherOpportunityId(), HttpMethod.PUT, open, admin, 200);
    }

    private Map<String, Object> opportunity(CommercialFixtures.Organization organization) {
        var request = new HashMap<String, Object>();
        request.put("title", "Oportunidad con oferta histórica");
        request.put("companyId", organization.companyId());
        request.put("contactId", organization.contactId());
        request.put("venueId", organization.venueId());
        request.put("eventTypeId", organization.eventTypeId());
        request.put("eventStart", "2030-01-01T12:00:00Z");
        request.put("eventEnd", "2030-01-01T16:00:00Z");
        request.put("attendeeCount", 20);
        return request;
    }

    @Test
    void invalidCapacityMoneyStateAndEquipmentDoNotWrite() {
        var a = CommercialFixtures.create(jdbc, encoder, "offer.validation").first();
        var admin = login(a.admin());
        int beforeVenues = jdbc.queryForObject("SELECT count(*) FROM venues WHERE tenant_id = ?", Integer.class, a.id());
        int beforeServices = jdbc.queryForObject("SELECT count(*) FROM event_services WHERE tenant_id = ?", Integer.class, a.id());
        for (var invalid : List.<Map<String, Object>>of(Map.of("name", " "), Map.of("capacity", 0),
                Map.of("capacity", -1), Map.of("rate", -1), Map.of("rate", "1.001"),
                Map.of("rate", "10000000000000"), Map.of("status", "DESCONOCIDO"), Map.of("equipment", List.of(" ")))) {
            var request = venue();
            request.putAll(invalid);
            send("venues", HttpMethod.POST, request, admin, 400);
            send("venues/" + a.venueId(), HttpMethod.PUT, request, admin, 400);
        }
        for (var invalid : List.<Map<String, Object>>of(Map.of("name", " "), Map.of("price", -1),
                Map.of("price", "1.001"), Map.of("price", "10000000000000"), Map.of("active", "DESCONOCIDO"))) {
            var request = service();
            request.putAll(invalid);
            send("event-services", HttpMethod.POST, request, admin, 400);
        }
        var missing = venue();
        missing.remove("status");
        send("venues", HttpMethod.POST, missing, admin, 400);
        missing = service();
        missing.remove("active");
        send("event-services", HttpMethod.POST, missing, admin, 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM venues WHERE tenant_id = ?", Integer.class, a.id())).isEqualTo(beforeVenues);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_services WHERE tenant_id = ?", Integer.class, a.id())).isEqualTo(beforeServices);
        assertThat(jdbc.queryForObject("SELECT capacity FROM venues WHERE id = ?", Integer.class, a.venueId())).isEqualTo(80);
        // Precios opcionales y cero siguen siendo válidos conforme al modelo vigente.
        var optional = venue();
        optional.remove("rate");
        assertThat(send("venues", HttpMethod.POST, optional, admin, 201).getBody().get("rate")).isNull();
        optional = service();
        optional.remove("price");
        assertThat(send("event-services", HttpMethod.POST, optional, admin, 201).getBody().get("price")).isNull();
    }

    @Test
    void writesRequireAdminAndAllReadsAndWritesStayWithinTenant() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "offer.scope");
        var a = scenario.first();
        var b = scenario.second();
        long localService = id(send("event-services", HttpMethod.POST, service(), login(a.admin()), 201));
        long foreignService = id(send("event-services", HttpMethod.POST, service(), login(b.admin()), 201));
        for (var account : new CommercialFixtures.Account[]{a.admin(), a.manager(), a.seller()}) {
            var headers = login(account);
            send("venues/" + b.venueId(), HttpMethod.GET, null, headers, 404);
            send("event-services/" + foreignService, HttpMethod.GET, null, headers, 404);
            var venues = http.exchange("/api/v1/venues", HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<PageResponse<Map<String, Object>>>() {}).getBody().content();
            var services = http.exchange("/api/v1/event-services", HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<PageResponse<Map<String, Object>>>() {}).getBody().content();
            assertThat(venues).extracting(value -> ((Number) value.get("id")).longValue()).contains(a.venueId()).doesNotContain(b.venueId());
            assertThat(services).extracting(value -> ((Number) value.get("id")).longValue()).contains(localService).doesNotContain(foreignService);
            int status = account.equals(a.admin()) ? 404 : 403;
            send("venues/" + b.venueId(), HttpMethod.PUT, venue(), headers, status);
            send("event-services/" + foreignService, HttpMethod.PUT, service(), headers, status);
            if (!account.equals(a.admin())) {
                send("venues", HttpMethod.POST, venue(), headers, 403);
                send("event-services", HttpMethod.POST, service(), headers, 403);
                send("venues/" + a.venueId(), HttpMethod.PUT, venue(), headers, 403);
                send("event-services/" + localService, HttpMethod.PUT, service(), headers, 403);
            }
        }
        assertThat(jdbc.queryForObject("SELECT name FROM venues WHERE id = ?", String.class, b.venueId())).isEqualTo("Salón offer.scope.b");
        assertThat(jdbc.queryForObject("SELECT active FROM event_services WHERE id = ?", Boolean.class, foreignService)).isTrue();
    }

    @Test
    void paginatesSearchesLaterRecordsAndValidatesFiltersSizeAndSort() {
        var a = CommercialFixtures.create(jdbc, encoder, "offer.pages").first();
        var headers = login(a.seller());
        for (int index = 0; index < 25; index++) {
            String name = index == 24 ? "ZZZ Oferta singular" : "Oferta " + index;
            jdbc.update("INSERT INTO venues (tenant_id, name, capacity, status) VALUES (?, ?, 30, ?)",
                    a.id(), name, index % 2 == 0 ? "DISPONIBLE" : "MANTENIMIENTO");
            jdbc.update("INSERT INTO event_services (tenant_id, name, active) VALUES (?, ?, ?)", a.id(), name, index % 2 == 0);
        }
        for (String resource : new String[]{"venues", "event-services"}) {
            var first = send(resource, HttpMethod.GET, null, headers, 200).getBody();
            assertThat(first).containsEntry("size", 20).containsEntry("page", 0).containsEntry("totalPages", 2);
            assertThat((List<?>) first.get("content")).hasSize(20);
            var second = send(resource + "?page=1", HttpMethod.GET, null, headers, 200).getBody();
            assertThat((List<?>) second.get("content")).anySatisfy(value -> assertThat(value.toString()).contains("ZZZ Oferta singular"));
            var found = send(resource + "?q=SINGULAR", HttpMethod.GET, null, headers, 200).getBody();
            assertThat(found).containsEntry("totalElements", 1);
            assertThat((List<?>) found.get("content")).hasSize(1);
            assertThat(send(resource + "?size=100", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("size", 100);
            for (String invalid : new String[]{"size=101", "size=0", "page=-1", "sort=tenantId,asc", "sort=name,invalid"}) {
                send(resource + "?" + invalid, HttpMethod.GET, null, headers, 400);
            }
        }
        assertThat(send("venues?status=MANTENIMIENTO", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("totalElements", 12);
        assertThat(send("event-services?active=false", HttpMethod.GET, null, headers, 200).getBody()).containsEntry("totalElements", 12);
        send("venues?status=DESCONOCIDO", HttpMethod.GET, null, headers, 400);
        send("event-services?active=DESCONOCIDO", HttpMethod.GET, null, headers, 400);
    }

    private Map<String, Object> venue() {
        return new HashMap<>(Map.of("name", " Salón de prueba ", "capacity", 30, "rate", 2500.50,
                "status", "DISPONIBLE", "equipment", List.of("Audio")));
    }
    private Map<String, Object> service() {
        return new HashMap<>(Map.of("name", " Servicio de prueba ", "price", 0, "active", true));
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
        if (method == HttpMethod.PUT && path.matches("opportunities/\\d+") && payload instanceof Map<?,?> source) {
            var current = http.exchange("/api/v1/" + path, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Map<String,Object>>(){});
            var versioned = new HashMap<String,Object>(); source.forEach((key,value)->versioned.put(key.toString(),value));
            versioned.put("version", current.getBody().getOrDefault("version", 0)); payload = versioned;
        }
        var response = http.exchange("/api/v1/" + path, method, new HttpEntity<>(payload, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(response.getStatusCode().value()).as(method + " " + path + ": " + response.getBody()).isEqualTo(status);
        return response;
    }
    private long id(ResponseEntity<Map<String, Object>> response) {
        return ((Number) response.getBody().get("id")).longValue();
    }
}
