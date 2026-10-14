package com.ztech.crm.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import com.ztech.crm.shared.dto.PageResponse;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class CommercialFixturesIT {
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

    @Test
    void reusableFixturesExerciseAllRolesAndBothTenantsByListAndId() {
        var scenario = CommercialFixtures.create(jdbc, encoder, "scope");
        for (var organization : new CommercialFixtures.Organization[] {scenario.first(), scenario.second()}) {
            var foreign = organization == scenario.first() ? scenario.second() : scenario.first();
            for (var account : new CommercialFixtures.Account[] {
                    organization.admin(), organization.manager(), organization.seller(), organization.otherSeller()}) {
                HttpHeaders headers = login(account);
                var response = http.exchange("/api/v1/opportunities?size=100", HttpMethod.GET,
                        new HttpEntity<>(headers), new ParameterizedTypeReference<PageResponse<OpportunityResponse>>() { });
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                var ids = response.getBody().content().stream().map(OpportunityResponse::id).toList();
                if (account == organization.seller()) {
                    assertThat(ids).containsExactly(organization.ownOpportunityId());
                } else if (account == organization.otherSeller()) {
                    assertThat(ids).containsExactly(organization.otherOpportunityId());
                } else {
                    assertThat(ids).containsExactlyInAnyOrder(organization.ownOpportunityId(), organization.otherOpportunityId());
                }
                assertThat(http.exchange("/api/v1/opportunities/" + foreign.ownOpportunityId(), HttpMethod.GET,
                        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(http.exchange("/api/v1/companies/" + foreign.companyId(), HttpMethod.GET,
                        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(http.exchange("/api/v1/companies/" + organization.companyId(), HttpMethod.GET,
                        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }
    }

    private HttpHeaders login(CommercialFixtures.Account account) {
        var response = http.postForEntity("/api/v1/auth/login",
                new LoginRequest(account.email(), CommercialFixtures.PASSWORD), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().mustChangePassword()).isFalse();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
}
