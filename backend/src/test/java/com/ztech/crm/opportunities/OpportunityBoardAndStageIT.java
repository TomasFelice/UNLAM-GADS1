package com.ztech.crm.opportunities;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.catalogs.domain.enums.StageKind;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.opportunities.dto.request.ChangeStageRequest;
import com.ztech.crm.opportunities.dto.request.CreateOpportunityRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityBoardResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** BE-OPP-05/06: cambio de etapa con historial, y tablero agrupado por etapa. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class OpportunityBoardAndStageIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders;
    private Long adminUserId;
    private Long firstOpenStageId;
    private Long secondOpenStageId;
    private Long wonStageId;
    private Long venueId;

    @BeforeEach
    void setUp() {
        LoginRequest loginRequest = new LoginRequest("admin@ztech.local", "Admin123!");
        LoginResponse login = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, LoginResponse.class)
                .getBody();
        adminUserId = login.user().id();

        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(login.accessToken());

        List<StageResponse> stages = restTemplate.exchange("/api/v1/stages", HttpMethod.GET,
                new HttpEntity<>(authHeaders), new ParameterizedTypeReference<List<StageResponse>>() {
                }).getBody();
        List<StageResponse> openStages = stages.stream().filter(s -> s.kind() == StageKind.OPEN).toList();
        firstOpenStageId = openStages.get(0).id();
        secondOpenStageId = openStages.get(1).id();
        wonStageId = stages.stream().filter(s -> s.kind() == StageKind.WON).findFirst().orElseThrow().id();

        PageResponse<VenueResponse> venues = restTemplate.exchange("/api/v1/venues", HttpMethod.GET,
                new HttpEntity<>(authHeaders), new ParameterizedTypeReference<PageResponse<VenueResponse>>() {
                }).getBody();
        venueId = venues.content().get(0).id();
    }

    @Test
    void changesStageAndOpportunityMovesOnTheBoard() {
        Long opportunityId = createOpportunity("Oportunidad a mover", firstOpenStageId);

        ResponseEntity<OpportunityResponse> changeResponse = restTemplate.exchange(
                "/api/v1/opportunities/" + opportunityId + "/stage", HttpMethod.POST,
                new HttpEntity<>(new ChangeStageRequest(secondOpenStageId, "Avanza"), authHeaders),
                OpportunityResponse.class);

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().stageId()).isEqualTo(secondOpenStageId);

        ResponseEntity<OpportunityBoardResponse> board = restTemplate.exchange("/api/v1/opportunities/board",
                HttpMethod.GET, new HttpEntity<>(authHeaders), OpportunityBoardResponse.class);

        assertThat(board.getStatusCode()).isEqualTo(HttpStatus.OK);
        OpportunityBoardResponse.StageColumn destinationColumn = board.getBody().columns().stream()
                .filter(c -> c.stageId().equals(secondOpenStageId)).findFirst().orElseThrow();
        assertThat(destinationColumn.opportunities()).extracting(OpportunityResponse::id).contains(opportunityId);

        OpportunityBoardResponse.StageColumn originColumn = board.getBody().columns().stream()
                .filter(c -> c.stageId().equals(firstOpenStageId)).findFirst().orElseThrow();
        assertThat(originColumn.opportunities()).extracting(OpportunityResponse::id).doesNotContain(opportunityId);
    }

    @Test
    void rejectsMovingToTheSameStage() {
        Long opportunityId = createOpportunity("Oportunidad misma etapa", firstOpenStageId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/opportunities/" + opportunityId + "/stage", HttpMethod.POST,
                new HttpEntity<>(new ChangeStageRequest(firstOpenStageId, null), authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void rejectsMovingDirectlyToAWonStage() {
        // Ganar una oportunidad es un caso de uso propio (/win, Fase 7) — el endpoint
        // genérico de cambio de etapa no permite saltar directo a una etapa cerrada.
        Long opportunityId = createOpportunity("Oportunidad a etapa ganada", firstOpenStageId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/opportunities/" + opportunityId + "/stage", HttpMethod.POST,
                new HttpEntity<>(new ChangeStageRequest(wonStageId, null), authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void returnsNotFoundForUnknownOpportunity() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/opportunities/999999/stage", HttpMethod.POST,
                new HttpEntity<>(new ChangeStageRequest(secondOpenStageId, null), authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void boardIncludesAllStagesAsColumnsEvenWhenEmpty() {
        ResponseEntity<OpportunityBoardResponse> board = restTemplate.exchange("/api/v1/opportunities/board",
                HttpMethod.GET, new HttpEntity<>(authHeaders), OpportunityBoardResponse.class);

        assertThat(board.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(board.getBody().columns()).hasSize(7);
    }

    private Long createOpportunity(String title, Long stageId) {
        Long companyId = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(new CompanyRequest(title + " - Empresa", null, null, null, null, null, null,
                        PartyStatus.POTENCIAL, null, null, null), authHeaders),
                CompanyResponse.class).getBody().id();

        CreateOpportunityRequest request = new CreateOpportunityRequest(title, companyId, null, adminUserId, venueId,
                stageId, null, null, Instant.now().plus(15, ChronoUnit.DAYS), 20, null, null, null);

        return restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), OpportunityResponse.class).getBody().id();
    }
}
