package com.ztech.crm.activities;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.activities.dto.response.ActivityResponse;
import com.ztech.crm.opportunities.dto.response.StageHistoryResponse;
import com.ztech.crm.shared.dto.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
class CommercialReadIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl); registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    @Test void v4SeedsACompleteCoherentScenario() {
        assertThat(jdbc.queryForObject("select count(*) from companies", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from contacts", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from opportunities", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select count(distinct stage_id) from opportunities", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select count(*) from activities", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from opportunities o join venues v on v.id=o.venue_id where o.attendee_count > v.capacity", Integer.class)).isZero();
    }

    @Test void catalogActivitiesAndHistoryExposeResolvedFacts() {
        HttpEntity<Void> request = authorized("admin@ztech.local");
        ResponseEntity<PageResponse<ActivityResponse>> activities = rest.exchange("/api/v1/activities?size=3",
                HttpMethod.GET, request, new ParameterizedTypeReference<>() { });
        assertThat(activities.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activities.getBody()).isNotNull();
        assertThat(activities.getBody().content()).hasSize(3).allSatisfy(item -> {
            assertThat(item.type().name()).isNotBlank(); assertThat(item.author().firstName()).isNotBlank();
        });
        assertThat(activities.getBody().content()).extracting(ActivityResponse::occurredAt)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
        assertContains("/api/v1/origins", request, "Sitio web");
        assertContains("/api/v1/loss-reasons", request, "Precio");
        assertContains("/api/v1/activity-types", request, "Llamada");

        Long opportunityId = jdbc.queryForObject("select id from opportunities where title='Jornada anual Andina'", Long.class);
        Long companyId = jdbc.queryForObject("select company_id from opportunities where id=?", Long.class, opportunityId);
        Long contactId = jdbc.queryForObject("select contact_id from opportunities where id=?", Long.class, opportunityId);
        String opportunityReference = "\"opportunityId\":" + opportunityId;
        assertContains("/api/v1/companies/" + companyId + "/activities", request, opportunityReference);
        assertContains("/api/v1/contacts/" + contactId + "/activities", request, opportunityReference);
        assertContains("/api/v1/opportunities/" + opportunityId + "/activities", request, opportunityReference);
        ResponseEntity<List<StageHistoryResponse>> history = rest.exchange(
                "/api/v1/opportunities/" + opportunityId + "/stage-history", HttpMethod.GET, request,
                new ParameterizedTypeReference<>() { });
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).singleElement().satisfies(item -> {
            assertThat(item.fromStage()).isNull(); assertThat(item.toStage().name()).isEqualTo("Consulta recibida");
            assertThat(item.author().firstName()).isNotBlank();
        });
    }

    @Test void sellerOnlyReadsOwnCommercialScopeAndFiltersCombine() {
        HttpEntity<Void> request = authorized("vendedor@ztech.local");
        ResponseEntity<String> response = rest.exchange(
                "/api/v1/opportunities?q=Andina&status=ABIERTA&salesRepId=2&size=20&sort=title,asc",
                HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Jornada anual Andina").doesNotContain("Presentación Nexo");
    }

    @Test void dp10FiltersWorkIndividuallyAndCombinedForEveryCommercialList() {
        HttpEntity<Void> request = authorized("admin@ztech.local");
        Long companyId = jdbc.queryForObject(
                "select id from companies where business_name='Andina Tech'", Long.class);
        Long contactId = jdbc.queryForObject(
                "select id from contacts where email='carolina@andinatech.example'", Long.class);
        Long originId = jdbc.queryForObject(
                "select origin_id from companies where id=?", Long.class, companyId);
        Long salesRepId = jdbc.queryForObject(
                "select sales_rep_id from companies where id=?", Long.class, companyId);
        Long opportunityId = jdbc.queryForObject(
                "select id from opportunities where title='Jornada anual Andina'", Long.class);
        Long stageId = jdbc.queryForObject(
                "select stage_id from opportunities where id=?", Long.class, opportunityId);
        Long venueId = jdbc.queryForObject(
                "select venue_id from opportunities where id=?", Long.class, opportunityId);

        for (String filter : List.of("q=Andina", "status=CLIENTE", "originId=" + originId,
                "salesRepId=" + salesRepId)) {
            assertContains("/api/v1/companies?" + filter, request, "Andina Tech");
        }
        for (String filter : List.of("q=Carolina", "status=CLIENTE", "originId=" + originId,
                "salesRepId=" + salesRepId, "companyId=" + companyId)) {
            assertContains("/api/v1/contacts?" + filter, request, "Carolina");
        }
        List<String> opportunityFilters = List.of("q=Jornada", "status=ABIERTA", "stageId=" + stageId,
                "originId=" + originId, "salesRepId=" + salesRepId, "venueId=" + venueId,
                "companyId=" + companyId, "contactId=" + contactId,
                "eventFrom=2026-10-08T00:00:00Z", "eventTo=2026-10-09T00:00:00Z");
        for (String filter : opportunityFilters) {
            assertContains("/api/v1/opportunities?" + filter, request, "Jornada anual Andina");
        }
        String combined = "q=Jornada&status=ABIERTA&stageId=" + stageId + "&originId=" + originId
                + "&salesRepId=" + salesRepId + "&venueId=" + venueId + "&companyId=" + companyId
                + "&contactId=" + contactId + "&eventFrom=2026-10-08T00:00:00Z"
                + "&eventTo=2026-10-09T00:00:00Z";
        assertContains("/api/v1/opportunities?" + combined, request, "Jornada anual Andina");
        assertContains("/api/v1/opportunities/board?" + combined, request, "Jornada anual Andina");
    }

    @Test void rejectsOversizedPagesAndUnknownSorts() {
        HttpEntity<Void> request = authorized("admin@ztech.local");
        assertThat(rest.exchange("/api/v1/companies?size=101", HttpMethod.GET, request, String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.exchange("/api/v1/opportunities?sort=tenantId,asc", HttpMethod.GET, request, String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpEntity<Void> authorized(String email) {
        LoginResponse login = rest.postForObject("/api/v1/auth/login", new LoginRequest(email, "Admin123!"), LoginResponse.class);
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(login.accessToken()); return new HttpEntity<>(headers);
    }

    private void assertContains(String path, HttpEntity<Void> request, String expected) {
        ResponseEntity<String> response = rest.exchange(path, HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(path).contains(expected);
    }
}
