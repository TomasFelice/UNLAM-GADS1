package com.ztech.crm.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.dto.request.CreateUserRequest;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.request.ResetPasswordRequest;
import com.ztech.crm.access.dto.request.UpdateUserRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.enforce-password-change=true")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class UserAdministrationIT {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    CommercialFixtures.Scenario fixtures;

    @BeforeEach
    void fixtures(TestInfo info) {
        String key = Integer.toUnsignedString(info.getTestMethod().orElseThrow().getName().hashCode(), 36);
        fixtures = CommercialFixtures.create(jdbc, encoder, "access." + key);
    }

    @Test
    void allAdministrativeEndpointsRejectNonAdminsAndForeignTenants() {
        for (var organization : new CommercialFixtures.Organization[] {fixtures.first(), fixtures.second()}) {
            var foreign = organization == fixtures.first() ? fixtures.second() : fixtures.first();
            var update = new UpdateUserRequest(organization.seller().email(), "Nuevo", "Nombre", Role.SELLER, true);
            var create = new CreateUserRequest("new." + organization.id() + "@fixtures.invalid", "Nuevo", "Usuario", Role.SELLER, CommercialFixtures.PASSWORD);
            for (var actor : new CommercialFixtures.Account[] {organization.seller(), organization.manager()}) {
                var headers = login(actor);
                assertThat(request("/users", HttpMethod.GET, null, headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(request("/users/" + organization.seller().id(), HttpMethod.GET, null, headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(request("/users", HttpMethod.POST, create, headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(request("/users/" + organization.seller().id(), HttpMethod.PUT, update, headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(request("/users/" + organization.seller().id() + "/reset-password", HttpMethod.POST,
                        new ResetPasswordRequest(CommercialFixtures.PASSWORD), headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            }
            var admin = login(organization.admin());
            var list = request("/users?size=100", HttpMethod.GET, null, admin);
            assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(list.getBody()).contains(organization.seller().email(), organization.manager().email())
                    .doesNotContain(foreign.admin().email(), foreign.seller().email(), "passwordHash", "password_hash", CommercialFixtures.PASSWORD);
            String foreignPath = "/users/" + foreign.seller().id();
            assertThat(request(foreignPath, HttpMethod.GET, null, admin).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(request(foreignPath, HttpMethod.PUT, update, admin).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(request(foreignPath + "/reset-password", HttpMethod.POST,
                    new ResetPasswordRequest(CommercialFixtures.PASSWORD), admin).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(jdbc.queryForObject("SELECT first_name FROM users WHERE id = ?", String.class, foreign.seller().id())).isEqualTo("seller");
        }
    }

    @Test
    void createdAndResetPasswordsUseSaltedBcryptWithoutExposingCredentials() {
        var organization = fixtures.first();
        var admin = login(organization.admin());
        String[] hashes = new String[2];
        long firstUser = 0;
        for (int i = 0; i < 2; i++) {
            String email = "bcrypt." + organization.id() + "." + i + "@fixtures.invalid";
            var created = request("/users", HttpMethod.POST,
                    new CreateUserRequest(email, "Clave", "Segura", Role.SELLER, CommercialFixtures.PASSWORD), admin);
            assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(created.getBody()).doesNotContain(CommercialFixtures.PASSWORD, "passwordHash", "password_hash");
            hashes[i] = jdbc.queryForObject("SELECT password_hash FROM users WHERE email=?", String.class, email);
            assertThat(hashes[i]).startsWith("$2").isNotEqualTo(CommercialFixtures.PASSWORD);
            assertThat(encoder.matches(CommercialFixtures.PASSWORD, hashes[i])).isTrue();
            if (i == 0) firstUser = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        }
        assertThat(hashes[0]).isNotEqualTo(hashes[1]);
        assertThat(request("/users/" + firstUser + "/reset-password", HttpMethod.POST,
                new ResetPasswordRequest("OtraTemporal123!"), admin).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String reset = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, firstUser);
        assertThat(reset).startsWith("$2").isNotEqualTo(hashes[0]);
        assertThat(encoder.matches("OtraTemporal123!", reset)).isTrue();
        assertThat(encoder.matches(CommercialFixtures.PASSWORD, reset)).isFalse();
    }

    @Test
    void protectsSelfAndLastAdminAndPreservesAuthorshipAfterDeactivation() {
        var organization = fixtures.first();
        var admin = login(organization.admin());
        var self = new UpdateUserRequest(organization.admin().email(), "admin", "Fixture", Role.ADMIN, false);
        assertThat(request("/users/" + organization.admin().id(), HttpMethod.PUT, self, admin).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var demotion = new UpdateUserRequest(organization.admin().email(), "admin", "Fixture", Role.SELLER, true);
        assertThat(request("/users/" + organization.admin().id(), HttpMethod.PUT, demotion, admin).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var seller = login(organization.otherSeller());
        String historyPath = "/opportunities/" + organization.otherOpportunityId() + "/stage-history";
        String activityPath = "/opportunities/" + organization.otherOpportunityId() + "/activities";
        String history = request(historyPath, HttpMethod.GET, null, admin).getBody();
        String activities = request(activityPath, HttpMethod.GET, null, admin).getBody();
        var deactivate = new UpdateUserRequest(organization.otherSeller().email(), "other", "Fixture", Role.SELLER, false);
        assertThat(request("/users/" + organization.otherSeller().id(), HttpMethod.PUT, deactivate, admin).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request("/users/assignable", HttpMethod.GET, null, seller).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.postForEntity("/api/v1/auth/login", new LoginRequest(organization.otherSeller().email(), CommercialFixtures.PASSWORD), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(request(historyPath, HttpMethod.GET, null, admin).getBody()).isEqualTo(history);
        assertThat(request(activityPath, HttpMethod.GET, null, admin).getBody()).isEqualTo(activities);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Integer.class, organization.otherSeller().id())).isEqualTo(1);
    }

    private HttpHeaders login(CommercialFixtures.Account account) {
        var response = http.postForEntity("/api/v1/auth/login", new LoginRequest(account.email(), CommercialFixtures.PASSWORD), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }

    private ResponseEntity<String> request(String path, HttpMethod method, Object body, HttpHeaders headers) {
        return http.exchange("/api/v1" + path, method, new HttpEntity<>(body, headers), String.class);
    }
}
