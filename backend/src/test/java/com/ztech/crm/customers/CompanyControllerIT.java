package com.ztech.crm.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.customers.dto.request.CompanyRequest;
import com.ztech.crm.customers.dto.request.ContactRequest;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.Map;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** BE-CUS-01..03: alta, edición, listado y detalle de empresas (con sus contactos). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class CompanyControllerIT {

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

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;

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
    void createsAndFetchesCompanyDetail() {
        CompanyRequest request = new CompanyRequest("Eventos del Sur SA", "30-81234567-9", "Turismo",
                "contacto@eventosdelsur.com", "+54 11 4000-0000", "Av. Rivadavia 4500", "https://eventosdelsur.com",
                PartyStatus.POTENCIAL, null, null, "Cliente potencial grande");

        ResponseEntity<CompanyResponse> createResponse = restTemplate.exchange(
                "/api/v1/companies", HttpMethod.POST, new HttpEntity<>(request, authHeaders), CompanyResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();
        Long id = createResponse.getBody().id();

        ResponseEntity<CompanyDetailResponse> detail = restTemplate.exchange(
                "/api/v1/companies/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders), CompanyDetailResponse.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().name()).isEqualTo("Eventos del Sur SA");
        assertThat(detail.getBody().cuit()).isEqualTo("30812345679");
    }

    @Test
    void updatesCompany() {
        CompanyRequest createRequest = new CompanyRequest("Empresa a editar", null, null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        Long id = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders), CompanyResponse.class).getBody().id();

        CompanyRequest updateRequest = new CompanyRequest("Empresa editada", null, null, null, null, null, null,
                PartyStatus.CLIENTE, null, null, "Ahora es cliente");

        ResponseEntity<CompanyResponse> updateResponse = restTemplate.exchange(
                "/api/v1/companies/" + id, HttpMethod.PUT, new HttpEntity<>(updateRequest, authHeaders), CompanyResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("Empresa editada");
        assertThat(updateResponse.getBody().status()).isEqualTo(PartyStatus.CLIENTE);
    }

    @Test
    void rejectsDuplicateCuitWithinTenant() {
        CompanyRequest first = new CompanyRequest("Primera SA", "30-99999999-5", null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        restTemplate.exchange("/api/v1/companies", HttpMethod.POST, new HttpEntity<>(first, authHeaders), CompanyResponse.class);

        CompanyRequest duplicate = new CompanyRequest("Segunda SA", "30999999995", null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/companies", HttpMethod.POST, new HttpEntity<>(duplicate, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsInvalidCuitFormat() {
        CompanyRequest request = new CompanyRequest("Empresa con CUIT inválido", "no-es-un-cuit", null, null, null,
                null, null, PartyStatus.POTENCIAL, null, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/companies", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validatesCheckDigitAndRejectsMalformedPresentationsWithoutWriting() {
        int before = jdbc.queryForObject("SELECT count(*) FROM companies", Integer.class);
        for (String cuit : new String[]{"30-81234567-8", "30-812345679", "3081234567-9",
                "30-8123 567-9", "30-81234567-99", "30-00000004-0"}) {
            var response = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                    new HttpEntity<>(Map.of("legalName", "Inválida", "businessName", "Inválida",
                            "status", "POTENCIAL", "cuit", cuit), authHeaders), String.class);
            assertThat(response.getStatusCode()).as(cuit).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("cuit", "VALIDATION_ERROR");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM companies", Integer.class)).isEqualTo(before);
    }

    @Test
    void canonicalizesBeforeUniquenessChecksOnCreateAndUpdateWithinEachTenant() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "cuit");
        var first = fixtureLogin(scenario.first().admin());
        var second = fixtureLogin(scenario.second().admin());
        var payload = Map.of("legalName", "CUIT válido", "businessName", "CUIT válido",
                "status", "POTENCIAL", "cuit", " 30-81234567-9 ");
        var created = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(payload, first), CompanyResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().cuit()).isEqualTo("30812345679");
        Long id = created.getBody().id();
        var canonical = Map.of("legalName", "Sin duplicar", "businessName", "Sin duplicar",
                "status", "CLIENTE", "cuit", "30812345679");
        assertThat(restTemplate.exchange("/api/v1/companies/" + id, HttpMethod.PUT,
                new HttpEntity<>(canonical, first), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(canonical, first), String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(restTemplate.exchange("/api/v1/companies/" + scenario.first().companyId(), HttpMethod.PUT,
                new HttpEntity<>(payload, first), String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT cuit FROM companies WHERE id = ?", String.class, scenario.first().companyId())).isNull();
        assertThat(restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(canonical, second), String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Resto cero: el verificador es cero, no once.
        var zero = Map.of("legalName", "Verificador cero", "businessName", "Verificador cero",
                "status", "POTENCIAL", "cuit", "30-74567890-0");
        assertThat(restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(zero, first), String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void normalizesOptionalBlanksBeforeValidationAndValidatesEmailAndWebsite() {
        var blank = Map.of("legalName", "Sin opcionales", "businessName", "Sin opcionales", "status", "POTENCIAL",
                "cuit", " \t ", "email", " \t ", "website", " \t ", "phone", " \t ", "notes", " \t ");
        var created = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(blank, authHeaders), CompanyResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var detail = restTemplate.exchange("/api/v1/companies/" + created.getBody().id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders), CompanyDetailResponse.class).getBody();
        assertThat(detail.cuit()).isNull();
        assertThat(detail.email()).isNull();
        assertThat(detail.website()).isNull();
        assertThat(detail.phone()).isNull();
        assertThat(detail.notes()).isNull();
        for (var invalid : new String[][]{{"email", "correo-invalido"}, {"website", "empresa.com"},
                {"website", "https://"}, {"website", "javascript:alert(1)"}, {"website", "https://emp resa.com"}}) {
            var response = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                    new HttpEntity<>(Map.of("legalName", "Formato", "businessName", "Formato", "status", "POTENCIAL",
                            invalid[0], invalid[1]), authHeaders), String.class);
            assertThat(response.getStatusCode()).as(invalid[1]).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains(invalid[0], "VALIDATION_ERROR");
        }
        var valid = Map.of("legalName", "Formatos válidos", "businessName", "Formatos válidos", "status", "POTENCIAL",
                "email", " correo@fixtures.invalid ", "website", " https://empresa.example/eventos?origen=web ");
        var saved = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(valid, authHeaders), CompanyResponse.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saved.getBody().email()).isEqualTo("correo@fixtures.invalid");
    }

    private HttpHeaders fixtureLogin(CommercialFixtures.Account account) {
        var login = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(account.email(), CommercialFixtures.PASSWORD), LoginResponse.class).getBody();
        var headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());
        return headers;
    }

    @Test
    void returnsNotFoundForUnknownId() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/companies/999999", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void detailAndPagedContactsExposeRelatedPeople() {
        CompanyRequest companyRequest = new CompanyRequest("Empresa con contactos", null, null, null, null, null,
                null, PartyStatus.POTENCIAL, null, null, null);
        Long companyId = restTemplate.exchange("/api/v1/companies", HttpMethod.POST,
                new HttpEntity<>(companyRequest, authHeaders), CompanyResponse.class).getBody().id();

        ContactRequest contactRequest = new ContactRequest(companyId, "María", "Gómez", null, "Compras", null, null,
                PartyStatus.POTENCIAL, null, null, null);
        restTemplate.exchange("/api/v1/contacts", HttpMethod.POST,
                new HttpEntity<>(contactRequest, authHeaders), ContactResponse.class);

        ResponseEntity<CompanyDetailResponse> detail = restTemplate.exchange(
                "/api/v1/companies/" + companyId, HttpMethod.GET, new HttpEntity<>(authHeaders), CompanyDetailResponse.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        var contacts = restTemplate.exchange("/api/v1/contacts?companyId="+companyId, HttpMethod.GET,
                new HttpEntity<>(authHeaders), new ParameterizedTypeReference<PageResponse<ContactResponse>>(){}).getBody();
        assertThat(contacts.totalElements()).isEqualTo(1);
        assertThat(contacts.content().getFirst().firstName()).isEqualTo("María");
    }

    @Test
    void listsCompaniesPaginated() {
        CompanyRequest request = new CompanyRequest("Empresa para el listado", null, null, null, null, null, null,
                PartyStatus.POTENCIAL, null, null, null);
        restTemplate.exchange("/api/v1/companies", HttpMethod.POST, new HttpEntity<>(request, authHeaders), CompanyResponse.class);

        ResponseEntity<PageResponse<CompanyResponse>> response = restTemplate.exchange(
                "/api/v1/companies?page=0&size=5", HttpMethod.GET, new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isNotEmpty();
        assertThat(response.getBody().size()).isEqualTo(5);
    }
}
