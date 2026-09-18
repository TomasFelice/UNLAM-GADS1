package com.ztech.crm.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
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

/**
 * Prueba el login (BE-SEC-01) y el rechazo de requests sin token (BE-SEC-02) contra un
 * Postgres real, con la migración V1 corriendo desde una base vacía — no contra
 * mocks. Cubre design.md §13 y las tareas de la Fase 1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

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

    @Test
    void loginWithSeedAdminReturnsToken() {
        LoginRequest request = new LoginRequest("admin@ztech.local", "Admin123!");

        ResponseEntity<LoginResponse> response =
                restTemplate.postForEntity("/api/v1/auth/login", request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().user().email()).isEqualTo("admin@ztech.local");
        assertThat(response.getBody().user().role()).isEqualTo("ADMIN");
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {
        LoginRequest request = new LoginRequest("admin@ztech.local", "contraseña-incorrecta");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/login", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() {
        // Mismo código que password incorrecta — no distingue "no existe" de
        // "contraseña incorrecta" (BE-SEC-01/BE-SEC-10).
        LoginRequest request = new LoginRequest("no-existe@ztech.local", "cualquier-cosa123");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/login", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithValidTokenListsUsers() {
        LoginRequest loginRequest = new LoginRequest("admin@ztech.local", "Admin123!");
        LoginResponse login = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, LoginResponse.class)
                .getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users", HttpMethod.GET, new HttpEntity<Void>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void apiDocsAndSwaggerUiArePublicWithoutToken() {
        // Regresión: "/v3/api-docs/**" NO cubre "/v3/api-docs.yaml" (sufijo en el
        // mismo segmento, no un sub-path) — SecurityConfig necesita las tres variantes
        // explícitas, si no esto vuelve a dar 401.
        assertThat(restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/v3/api-docs.yaml", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/swagger-ui.html", String.class).getStatusCode())
                .isIn(HttpStatus.OK, HttpStatus.FOUND);
    }
}
