package com.ztech.crm.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.request.ContactRequest;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.dto.response.ContactDetailResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
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

/** BE-CUS-04..07: alta, edición, listado y detalle de contactos, con o sin empresa. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class ContactControllerIT {

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

    @BeforeEach
    void login() {
        LoginRequest loginRequest = new LoginRequest("admin@ztech.local", "Admin123!");
        LoginResponse login = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, LoginResponse.class)
                .getBody();

        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(login.accessToken());
    }

    @Test
    void createsIndividualContactWithoutCompany() {
        ContactRequest request = new ContactRequest(null, "Juan", "Pérez", "30111222", null,
                "juan.perez@mail.com", null, PartyStatus.POTENCIAL, null, null, null);

        ResponseEntity<ContactResponse> response = restTemplate.exchange(
                "/api/v1/contacts", HttpMethod.POST, new HttpEntity<>(request, authHeaders), ContactResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().companyId()).isNull();
    }

    @Test
    void createsContactLinkedToCompanyAndFetchesDetail() {
        Long companyId = createCompany("Empresa vinculada");

        ContactRequest request = new ContactRequest(companyId, "Ana", "López", null, "Gerente",
                "ana.lopez@empresa.com", null, PartyStatus.CLIENTE, null, null, null);
        Long contactId = restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), ContactResponse.class).getBody().id();

        ResponseEntity<ContactDetailResponse> detail = restTemplate.exchange(
                "/api/v1/contacts/" + contactId, HttpMethod.GET, new HttpEntity<>(authHeaders), ContactDetailResponse.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().company().id()).isEqualTo(companyId);
        assertThat(detail.getBody().position()).isEqualTo("Gerente");
    }

    @Test
    void rejectsContactWithNonExistentCompany() {
        ContactRequest request = new ContactRequest(999999L, "Sin", "Empresa", null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/contacts", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsDuplicateDocumentWithinTenant() {
        ContactRequest first = new ContactRequest(null, "Primero", "Uno", "40555666", null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        restTemplate.exchange("/api/v1/contacts", HttpMethod.POST, new HttpEntity<>(first, authHeaders), ContactResponse.class);

        ContactRequest duplicate = new ContactRequest(null, "Segundo", "Dos", "40555666", null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/contacts", HttpMethod.POST, new HttpEntity<>(duplicate, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updatesContactUnlinkingItFromCompany() {
        Long companyId = createCompany("Empresa a desvincular");
        ContactRequest createRequest = new ContactRequest(companyId, "Carlos", "Ruiz", null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        Long contactId = restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders), ContactResponse.class).getBody().id();

        ContactRequest updateRequest = new ContactRequest(null, "Carlos", "Ruiz", null, null, null, null,
                PartyStatus.CLIENTE, null, null, "Pasó a ser cliente individual");

        ResponseEntity<ContactResponse> updateResponse = restTemplate.exchange(
                "/api/v1/contacts/" + contactId, HttpMethod.PUT, new HttpEntity<>(updateRequest, authHeaders), ContactResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().companyId()).isNull();
        assertThat(updateResponse.getBody().status()).isEqualTo(PartyStatus.CLIENTE);
    }

    @Test
    void normalizesBlankOptionalsAndRejectsNonBlankInvalidEmail() {
        var blank = Map.of("firstName", "Sin", "lastName", "Opcionales", "status", "POTENCIAL",
                "email", " \t ", "document", " \t ", "phone", " \t ", "position", " \t ", "notes", " \t ");
        var response = restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(blank, authHeaders), ContactResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var detail = restTemplate.exchange("/api/v1/contacts/" + response.getBody().id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders), ContactDetailResponse.class).getBody();
        assertThat(detail.email()).isNull();
        assertThat(detail.document()).isNull();
        assertThat(detail.phone()).isNull();
        assertThat(detail.position()).isNull();
        assertThat(detail.notes()).isNull();
        var invalid = Map.of("firstName", "Correo", "lastName", "Inválido", "status", "POTENCIAL", "email", "sin-arroba");
        var rejected = restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(invalid, authHeaders), String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rejected.getBody()).contains("email", "VALIDATION_ERROR");
    }

    private Long createCompany(String name) {
        CompanyRequest request = new CompanyRequest(name, null, null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        return restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), CompanyResponse.class).getBody().id();
    }
}
