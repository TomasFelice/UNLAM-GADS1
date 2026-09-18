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
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.opportunities.dto.request.CreateOpportunityRequest;
import com.ztech.crm.opportunities.dto.request.UpdateOpportunityRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityDetailResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import java.math.BigDecimal;
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

/**
 * BE-OPP-01..03. De paso ejercita {@code GET /venues} y {@code GET /stages}
 * (BE-OFF-01, BE-CAT-01): los usa para armar los datos del test contra el seed real de
 * V1 (7 etapas, 3 salones).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class OpportunityControllerIT {

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
    private Long openStageId;
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
        openStageId = stages.stream().filter(s -> s.kind() == StageKind.OPEN).findFirst().orElseThrow().id();
        wonStageId = stages.stream().filter(s -> s.kind() == StageKind.WON).findFirst().orElseThrow().id();

        List<VenueResponse> venues = restTemplate.exchange("/api/v1/venues", HttpMethod.GET,
                new HttpEntity<>(authHeaders), new ParameterizedTypeReference<List<VenueResponse>>() {
                }).getBody();
        venueId = venues.get(0).id();
    }

    @Test
    void createsOpportunityWithCompanyAndFetchesDetail() {
        Long companyId = createCompany("Cliente con oportunidad");
        CreateOpportunityRequest request = new CreateOpportunityRequest("Evento fin de año", companyId, null,
                adminUserId, venueId, openStageId, new BigDecimal("500000.00"), 60,
                Instant.now().plus(30, ChronoUnit.DAYS), 20, null, null, "Primer contacto muy interesado");

        ResponseEntity<OpportunityResponse> createResponse = restTemplate.exchange("/api/v1/opportunities",
                HttpMethod.POST, new HttpEntity<>(request, authHeaders), OpportunityResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().status()).isEqualTo(OpportunityStatus.ABIERTA);
        Long id = createResponse.getBody().id();

        ResponseEntity<OpportunityDetailResponse> detail = restTemplate.exchange("/api/v1/opportunities/" + id,
                HttpMethod.GET, new HttpEntity<>(authHeaders), OpportunityDetailResponse.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().company().id()).isEqualTo(companyId);
        assertThat(detail.getBody().contact()).isNull();
        assertThat(detail.getBody().venue().id()).isEqualTo(venueId);
        assertThat(detail.getBody().stage().id()).isEqualTo(openStageId);
    }

    @Test
    void rejectsOpportunityWithoutCompanyOrContact() {
        CreateOpportunityRequest request = new CreateOpportunityRequest("Sin cliente", null, null, adminUserId,
                venueId, openStageId, null, null, Instant.now().plus(10, ChronoUnit.DAYS), 20, null, null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void rejectsOpportunityStartingInAClosedStage() {
        Long companyId = createCompany("Cliente con etapa cerrada");
        CreateOpportunityRequest request = new CreateOpportunityRequest("Etapa inválida", companyId, null,
                adminUserId, venueId, wonStageId, null, null, Instant.now().plus(10, ChronoUnit.DAYS), 20, null,
                null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void rejectsOpportunityWithNonExistentVenue() {
        Long companyId = createCompany("Cliente con salón inexistente");
        CreateOpportunityRequest request = new CreateOpportunityRequest("Salón inválido", companyId, null,
                adminUserId, 999999L, openStageId, null, null, Instant.now().plus(10, ChronoUnit.DAYS), 20, null,
                null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updatesOpenOpportunity() {
        Long companyId = createCompany("Cliente a editar");
        CreateOpportunityRequest createRequest = new CreateOpportunityRequest("Título original", companyId, null,
                adminUserId, venueId, openStageId, null, null, Instant.now().plus(10, ChronoUnit.DAYS), 15, null,
                null, null);
        Long id = restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders), OpportunityResponse.class).getBody().id();

        UpdateOpportunityRequest updateRequest = new UpdateOpportunityRequest("Título actualizado", companyId, null,
                venueId, new BigDecimal("800000.00"), 75, Instant.now().plus(45, ChronoUnit.DAYS), 25, null, null,
                "Ajustado tras la visita");

        ResponseEntity<OpportunityResponse> updateResponse = restTemplate.exchange("/api/v1/opportunities/" + id,
                HttpMethod.PUT, new HttpEntity<>(updateRequest, authHeaders), OpportunityResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().title()).isEqualTo("Título actualizado");
        assertThat(updateResponse.getBody().attendeeCount()).isEqualTo(25);
    }

    @Test
    void listsOpportunitiesPaginated() {
        Long companyId = createCompany("Cliente para el listado");
        CreateOpportunityRequest request = new CreateOpportunityRequest("Oportunidad listada", companyId, null,
                adminUserId, venueId, openStageId, null, null, Instant.now().plus(5, ChronoUnit.DAYS), 10, null,
                null, null);
        restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST, new HttpEntity<>(request, authHeaders),
                OpportunityResponse.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/opportunities?page=0&size=5",
                HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Long createCompany(String name) {
        CompanyRequest request = new CompanyRequest(name, null, null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        return restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), CompanyResponse.class).getBody().id();
    }
}
