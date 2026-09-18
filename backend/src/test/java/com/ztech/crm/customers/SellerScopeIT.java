package com.ztech.crm.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.dto.request.CreateUserRequest;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.access.dto.response.UserResponse;
import com.ztech.crm.catalogs.dto.response.EventTypeResponse;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.request.ContactRequest;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.opportunities.dto.request.ChangeStageRequest;
import com.ztech.crm.opportunities.dto.request.CreateOpportunityRequest;
import com.ztech.crm.opportunities.dto.response.OpportunityBoardResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.shared.dto.PageResponse;
import java.time.Instant;
import java.util.Set;
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

/** BE-SEC-04/06: tenant y alcance comercial en listados, ids directos y escrituras. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class SellerScopeIT {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sellerReadsDirectAndOpportunityCustomersButEditsOnlyDirectAssignments() {
        HttpHeaders admin = bearer(login("admin@ztech.local", "Admin123!"));
        UserResponse seller = createSeller(admin, "alcance.uno@ztech.local", "Alcance");
        UserResponse otherSeller = createSeller(admin, "alcance.dos@ztech.local", "Otro");

        CompanyResponse directCompany = createCompany(admin, "Empresa directa", seller.id());
        ContactResponse directContact = createContact(admin, directCompany.id(), "Contacto", "Directo", seller.id());

        CompanyResponse relatedCompany = createCompany(admin, "Empresa por oportunidad", otherSeller.id());
        ContactResponse relatedContact = createContact(
                admin, relatedCompany.id(), "Contacto", "Relacionado", otherSeller.id());
        ContactResponse hiddenSibling = createContact(
                admin, relatedCompany.id(), "Contacto", "No relacionado", otherSeller.id());

        CompanyResponse unrelatedCompany = createCompany(admin, "Empresa ajena", otherSeller.id());
        ContactResponse unrelatedContact = createContact(
                admin, unrelatedCompany.id(), "Contacto", "Ajeno", otherSeller.id());

        OpportunityResponse ownOpportunity = createOpportunity(
                admin, "Oportunidad propia", relatedCompany.id(), relatedContact.id(), seller.id(), 30);
        OpportunityResponse otherOpportunity = createOpportunity(
                admin, "Oportunidad ajena", unrelatedCompany.id(), unrelatedContact.id(), otherSeller.id(), 60);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stage_history WHERE opportunity_id = ?", Integer.class, ownOpportunity.id()))
                .isEqualTo(1);

        HttpHeaders sellerHeaders = bearer(login("alcance.uno@ztech.local", "Temporal123!"));

        PageResponse<CompanyResponse> companies = listCompanies(sellerHeaders);
        assertThat(companies.content()).extracting(CompanyResponse::id)
                .contains(directCompany.id(), relatedCompany.id())
                .doesNotContain(unrelatedCompany.id());

        PageResponse<ContactResponse> contacts = listContacts(sellerHeaders);
        assertThat(contacts.content()).extracting(ContactResponse::id)
                .contains(directContact.id(), relatedContact.id())
                .doesNotContain(hiddenSibling.id(), unrelatedContact.id());

        ResponseEntity<CompanyDetailResponse> relatedDetail = restTemplate.exchange(
                "/api/v1/companies/" + relatedCompany.id(), HttpMethod.GET,
                new HttpEntity<Void>(sellerHeaders), CompanyDetailResponse.class);
        assertThat(relatedDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(relatedDetail.getBody().contacts()).extracting(contact -> contact.id())
                .containsExactly(relatedContact.id());

        assertNotFound("/api/v1/companies/" + unrelatedCompany.id(), sellerHeaders);
        assertNotFound("/api/v1/contacts/" + unrelatedContact.id(), sellerHeaders);

        ResponseEntity<String> cannotEditRelatedCompany = restTemplate.exchange(
                "/api/v1/companies/" + relatedCompany.id(), HttpMethod.PUT,
                new HttpEntity<>(companyRequest("Intento de edición", otherSeller.id()), sellerHeaders), String.class);
        assertThat(cannotEditRelatedCompany.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> cannotEditRelatedContact = restTemplate.exchange(
                "/api/v1/contacts/" + relatedContact.id(), HttpMethod.PUT,
                new HttpEntity<>(contactRequest(relatedCompany.id(), "Intento", "Edición", otherSeller.id()),
                        sellerHeaders), String.class);
        assertThat(cannotEditRelatedContact.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<CompanyResponse> editsDirectCompany = restTemplate.exchange(
                "/api/v1/companies/" + directCompany.id(), HttpMethod.PUT,
                new HttpEntity<>(companyRequest("Empresa directa editada", seller.id()), sellerHeaders),
                CompanyResponse.class);
        assertThat(editsDirectCompany.getStatusCode()).isEqualTo(HttpStatus.OK);

        PageResponse<OpportunityResponse> opportunities = listOpportunities(sellerHeaders);
        assertThat(opportunities.content()).extracting(OpportunityResponse::id)
                .containsExactly(ownOpportunity.id());

        OpportunityBoardResponse board = restTemplate.exchange(
                "/api/v1/opportunities/board", HttpMethod.GET,
                new HttpEntity<Void>(sellerHeaders), OpportunityBoardResponse.class).getBody();
        assertThat(board.columns()).flatExtracting(column -> column.opportunities())
                .extracting(OpportunityResponse::id).containsExactly(ownOpportunity.id());

        assertNotFound("/api/v1/opportunities/" + otherOpportunity.id(), sellerHeaders);
        ResponseEntity<String> cannotMoveOtherOpportunity = restTemplate.exchange(
                "/api/v1/opportunities/" + otherOpportunity.id() + "/stage", HttpMethod.POST,
                new HttpEntity<>(new ChangeStageRequest(2L, null), sellerHeaders), String.class);
        assertThat(cannotMoveOtherOpportunity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ForeignIds foreign = insertCommercialDataInAnotherTenant();
        assertNotFound("/api/v1/companies/" + foreign.companyId(), sellerHeaders);
        assertNotFound("/api/v1/companies/" + foreign.companyId() + "/activities", sellerHeaders);
        assertNotFound("/api/v1/opportunities/" + foreign.opportunityId() + "/activities", sellerHeaders);
        assertNotFound("/api/v1/opportunities/" + foreign.opportunityId() + "/stage-history", sellerHeaders);
    }

    private UserResponse createSeller(HttpHeaders admin, String email, String firstName) {
        return restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest(email, firstName, "Vendedor",
                        Role.SELLER, "Temporal123!"), admin), UserResponse.class).getBody();
    }

    private CompanyResponse createCompany(HttpHeaders admin, String name, Long salesRepId) {
        return restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(companyRequest(name, salesRepId), admin), CompanyResponse.class).getBody();
    }

    private ContactResponse createContact(HttpHeaders admin, Long companyId, String firstName,
                                           String lastName, Long salesRepId) {
        return restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(contactRequest(companyId, firstName, lastName, salesRepId), admin),
                ContactResponse.class).getBody();
    }

    private OpportunityResponse createOpportunity(HttpHeaders admin, String title, Long companyId,
                                                    Long contactId, Long salesRepId, long dayOffset) {
        VenueResponse venue = restTemplate.exchange("/api/v1/venues", HttpMethod.GET,
                new HttpEntity<Void>(admin), VenueResponse[].class).getBody()[0];
        StageResponse stage = restTemplate.exchange("/api/v1/stages", HttpMethod.GET,
                new HttpEntity<Void>(admin), StageResponse[].class).getBody()[0];
        EventTypeResponse eventType = restTemplate.exchange("/api/v1/event-types", HttpMethod.GET,
                new HttpEntity<Void>(admin), EventTypeResponse[].class).getBody()[0];
        Instant start = Instant.parse("2030-01-01T12:00:00Z").plusSeconds(dayOffset * 86_400);
        CreateOpportunityRequest request = new CreateOpportunityRequest(title, companyId, contactId,
                salesRepId, venue.id(), stage.id(), eventType.id(), null, null, start,
                start.plusSeconds(14_400), 10, null, null, Set.of(), null);
        return restTemplate.exchange("/api/v1/opportunities", HttpMethod.POST,
                new HttpEntity<>(request, admin), OpportunityResponse.class).getBody();
    }

    private PageResponse<CompanyResponse> listCompanies(HttpHeaders headers) {
        return restTemplate.exchange("/api/v1/companies?size=100", HttpMethod.GET,
                new HttpEntity<Void>(headers), new ParameterizedTypeReference<PageResponse<CompanyResponse>>() {
                }).getBody();
    }

    private PageResponse<ContactResponse> listContacts(HttpHeaders headers) {
        return restTemplate.exchange("/api/v1/contacts?size=100", HttpMethod.GET,
                new HttpEntity<Void>(headers), new ParameterizedTypeReference<PageResponse<ContactResponse>>() {
                }).getBody();
    }

    private PageResponse<OpportunityResponse> listOpportunities(HttpHeaders headers) {
        return restTemplate.exchange("/api/v1/opportunities?size=100", HttpMethod.GET,
                new HttpEntity<Void>(headers), new ParameterizedTypeReference<PageResponse<OpportunityResponse>>() {
                }).getBody();
    }

    private CompanyRequest companyRequest(String name, Long salesRepId) {
        return new CompanyRequest(name + " SA", name, null, null, null, null, null,
                null, null, PartyStatus.POTENCIAL, salesRepId, null, null);
    }

    private ContactRequest contactRequest(Long companyId, String firstName, String lastName, Long salesRepId) {
        return new ContactRequest(companyId, firstName, lastName, null, null, null, null,
                PartyStatus.POTENCIAL, salesRepId, null, null);
    }

    private String login(String email, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, password), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private void assertNotFound(String path, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                path, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ForeignIds insertCommercialDataInAnotherTenant() {
        Long tenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenants (name) VALUES ('Tenant ajeno') RETURNING id", Long.class);
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active)
                VALUES (?, 'foreign.seller@ztech.local', 'unused', 'Foreign', 'Seller', 'SELLER', TRUE)
                RETURNING id
                """, Long.class, tenantId);
        Long companyId = jdbcTemplate.queryForObject("""
                INSERT INTO companies (tenant_id, legal_name, business_name, status, sales_rep_id)
                VALUES (?, 'Empresa ajena SA', 'Empresa ajena', 'POTENCIAL', ?)
                RETURNING id
                """, Long.class, tenantId, userId);
        Long venueId = jdbcTemplate.queryForObject("""
                INSERT INTO venues (tenant_id, name, capacity, status)
                VALUES (?, 'Salón ajeno', 100, 'DISPONIBLE') RETURNING id
                """, Long.class, tenantId);
        Long stageId = jdbcTemplate.queryForObject("""
                INSERT INTO stages (tenant_id, name, position, kind)
                VALUES (?, 'Etapa ajena', 1, 'OPEN') RETURNING id
                """, Long.class, tenantId);
        Long eventTypeId = jdbcTemplate.queryForObject("""
                INSERT INTO event_types (tenant_id, name)
                VALUES (?, 'Evento ajeno') RETURNING id
                """, Long.class, tenantId);
        Long opportunityId = jdbcTemplate.queryForObject("""
                INSERT INTO opportunities (
                    tenant_id, title, company_id, sales_rep_id, venue_id, stage_id,
                    event_type_id, status, event_start, event_end, attendee_count
                ) VALUES (?, 'Oportunidad ajena al tenant', ?, ?, ?, ?, ?, 'ABIERTA',
                    '2031-01-01T12:00:00Z', '2031-01-01T16:00:00Z', 20)
                RETURNING id
                """, Long.class, tenantId, companyId, userId, venueId, stageId, eventTypeId);
        return new ForeignIds(companyId, opportunityId);
    }

    private record ForeignIds(Long companyId, Long opportunityId) { }
}
