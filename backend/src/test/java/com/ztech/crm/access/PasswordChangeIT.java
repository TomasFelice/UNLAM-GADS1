package com.ztech.crm.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.ChangePasswordRequest;
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
 * Valida el circuito completo de contraseña temporal con la restricción habilitada.
 * Usa una base aislada para no cambiar las credenciales que consumen los IT históricos.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.enforce-password-change=true")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class PasswordChangeIT {

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
    void temporaryPasswordMustBeChangedAndPreviousTokenIsRevoked() {
        LoginResponse initialLogin = login("Admin123!");
        assertThat(initialLogin.mustChangePassword()).isTrue();

        HttpHeaders initialHeaders = bearer(initialLogin.accessToken());
        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/v1/users", HttpMethod.GET, new HttpEntity<Void>(initialHeaders), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(blocked.getBody()).contains("PASSWORD_CHANGE_REQUIRED");

        ResponseEntity<Void> changed = restTemplate.exchange(
                "/api/v1/auth/change-password",
                HttpMethod.POST,
                new HttpEntity<>(new ChangePasswordRequest("Admin123!", "NuevaClave123!"), initialHeaders),
                Void.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> revoked = restTemplate.exchange(
                "/api/v1/users", HttpMethod.GET, new HttpEntity<Void>(initialHeaders), String.class);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(revoked.getBody()).contains("TOKEN_REVOKED");

        LoginResponse renewedLogin = login("NuevaClave123!");
        assertThat(renewedLogin.mustChangePassword()).isFalse();

        ResponseEntity<String> allowed = restTemplate.exchange(
                "/api/v1/users", HttpMethod.GET,
                new HttpEntity<Void>(bearer(renewedLogin.accessToken())), String.class);
        assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changePasswordPreflightIsAllowedBeforeAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(java.util.List.of("authorization", "content-type"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/change-password", HttpMethod.OPTIONS,
                new HttpEntity<Void>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
        assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.POST);
        assertThat(response.getHeaders().getAccessControlAllowHeaders())
                .contains("authorization", "content-type");
    }

    private LoginResponse login(String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("admin@ztech.local", password),
                LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
