package com.ztech.crm.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.dto.request.CreateUserRequest;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.request.ResetPasswordRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.access.dto.response.UserResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class UserControllerIT {

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
    void adminCreatesAndReadsUserWhileSellerIsForbiddenAndResetRevokesToken() {
        String adminToken = login("admin@ztech.local", "Admin123!").accessToken();
        HttpHeaders adminHeaders = bearer(adminToken);

        ResponseEntity<UserResponse> created = restTemplate.exchange(
                "/api/v1/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("seller@ztech.local", "Sara", "López",
                        Role.SELLER, "Temporal123!"), adminHeaders), UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().mustChangePassword()).isTrue();

        Long sellerId = created.getBody().id();
        ResponseEntity<UserResponse> detail = restTemplate.exchange(
                "/api/v1/users/" + sellerId, HttpMethod.GET,
                new HttpEntity<Void>(adminHeaders), UserResponse.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().email()).isEqualTo("seller@ztech.local");

        String sellerToken = login("seller@ztech.local", "Temporal123!").accessToken();
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/v1/users", HttpMethod.GET,
                new HttpEntity<Void>(bearer(sellerToken)), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Void> reset = restTemplate.exchange(
                "/api/v1/users/" + sellerId + "/reset-password", HttpMethod.POST,
                new HttpEntity<>(new ResetPasswordRequest("OtraTemporal123!"), adminHeaders), Void.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> revoked = restTemplate.exchange(
                "/api/v1/users/assignable", HttpMethod.GET,
                new HttpEntity<Void>(bearer(sellerToken)), String.class);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private LoginResponse login(String email, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, password), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
