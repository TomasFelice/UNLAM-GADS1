package com.ztech.crm.support;

import com.ztech.crm.CrmApplication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Servidor local de medición: nunca usa .env ni una base persistente. Sólo classpath de test. */
public final class LoadBaselineApplication {
    private LoadBaselineApplication() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 3) {
            startServer(args);
            return;
        }
        try (var postgres = new PostgreSQLContainer("postgres:16-alpine"); var input = new Scanner(System.in)) {
            postgres.start();
            Process server = null;
            try {
                while (true) {
                    server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                            "-cp", System.getProperty("java.class.path"), LoadBaselineApplication.class.getName(),
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            .redirectError(ProcessBuilder.Redirect.INHERIT).start();
                    if (!input.hasNextLine() || !input.nextLine().equals("restart")) break;
                    server.destroy();
                    server.waitFor();
                    server = null;
                }
            } finally {
                if (server != null) {
                    server.destroy();
                    server.waitFor();
                }
            }
        }
    }

    private static void startServer(String[] database) {
        long started = System.nanoTime();
        ConfigurableApplicationContext context = new SpringApplication(CrmApplication.class, BaselineConfiguration.class).run(
                            "--spring.profiles.active=test", "--DB_URL=" + database[0],
                            "--DB_USERNAME=" + database[1], "--DB_PASSWORD=" + database[2],
                            "--server.address=127.0.0.1", "--server.port=18080",
                            "--app.cors.allowed-origins=http://localhost:14173",
                            "--app.security.enforce-password-change=true",
                            "--logging.level.root=ERROR", "--debug=false", "--spring.main.banner-mode=off",
                            "--spring.jpa.properties.hibernate.session_factory.statement_inspector=" + SqlCounter.class.getName());
        long startupMs = (System.nanoTime() - started) / 1_000_000;
        var jdbc = context.getBean(JdbcTemplate.class);
        if (jdbc.queryForObject("SELECT count(*) FROM tenants WHERE name = 'Fixture baseline.a'", Integer.class) == 0) {
            var dataset = CommercialFixtures.create(jdbc, context.getBean(PasswordEncoder.class), "baseline").first();
            enlargeDataset(jdbc, dataset);
        }
        long opportunityId = jdbc.queryForObject("SELECT id FROM opportunities WHERE title = 'baseline.a propia'", Long.class);
        System.out.printf("BASELINE_READY {\"startupMs\":%d,\"opportunityId\":%d,\"email\":\"admin.baseline.a@fixtures.invalid\"}%n",
                startupMs, opportunityId);
        System.out.flush();
    }

    private static void enlargeDataset(JdbcTemplate jdbc, CommercialFixtures.Organization data) {
        // 100 oportunidades y actividades, 30 empresas/contactos y cuatro servicios en el tenant medido.
        for (int index = 0; index < 98; index++) {
            jdbc.update("""
                    INSERT INTO opportunities (pipeline_id, tenant_id, title, company_id, contact_id, sales_rep_id,
                        venue_id, stage_id, event_type_id, status, event_start, event_end, attendee_count)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ABIERTA', '2030-01-01T12:00:00Z', '2030-01-01T16:00:00Z', 20)
                    """, data.pipelineId(), data.id(), "Evento de prueba " + index, data.companyId(), data.contactId(),
                    index % 2 == 0 ? data.seller().id() : data.otherSeller().id(), data.venueId(), data.stageId(), data.eventTypeId());
            jdbc.update("""
                    INSERT INTO activities (tenant_id, type_id, occurred_at, company_id, contact_id,
                                            opportunity_id, description, created_by)
                    SELECT ?, id, '2026-10-01T12:00:00Z', ?, ?, ?, 'Interacción de prueba', ?
                    FROM activity_types WHERE tenant_id = ? LIMIT 1
                    """, data.id(), data.companyId(), data.contactId(), data.ownOpportunityId(), data.seller().id(), data.id());
        }
        for (int index = 0; index < 29; index++) {
            long company = jdbc.queryForObject("""
                    INSERT INTO companies (tenant_id, legal_name, business_name, status, sales_rep_id)
                    VALUES (?, ?, ?, 'POTENCIAL', ?) RETURNING id
                    """, Long.class, data.id(), "Empresa de prueba " + index, "Empresa de prueba " + index, data.seller().id());
            jdbc.update("""
                    INSERT INTO contacts (tenant_id, company_id, first_name, last_name, status, sales_rep_id)
                    VALUES (?, ?, 'Contacto', ?, 'POTENCIAL', ?)
                    """, data.id(), company, "Prueba " + index, data.seller().id());
        }
        for (int index = 0; index < 4; index++) {
            jdbc.update("INSERT INTO event_services (tenant_id, name, price) VALUES (?, ?, 10000)",
                    data.id(), "Servicio de prueba " + index);
        }
    }

    public static final class SqlCounter implements StatementInspector {
        private static final ThreadLocal<Integer> COUNT = new ThreadLocal<>();

        @Override
        public String inspect(String sql) {
            Integer count = COUNT.get();
            if (count != null) COUNT.set(count + 1);
            return sql;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class BaselineConfiguration {
        @Bean
        FilterRegistrationBean<OncePerRequestFilter> baselineMetrics() {
            var registration = new FilterRegistrationBean<OncePerRequestFilter>();
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setFilter(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain chain) throws ServletException, IOException {
                    var wrapped = new ContentCachingResponseWrapper(response);
                    long started = System.nanoTime();
                    SqlCounter.COUNT.set(0);
                    try {
                        chain.doFilter(request, wrapped);
                        wrapped.setHeader("X-Baseline-Sql-Count", SqlCounter.COUNT.get().toString());
                        wrapped.setHeader("X-Baseline-Server-Ms", Long.toString((System.nanoTime() - started) / 1_000_000));
                        wrapped.copyBodyToResponse();
                    } finally {
                        SqlCounter.COUNT.remove();
                    }
                }
            });
            return registration;
        }
    }
}
