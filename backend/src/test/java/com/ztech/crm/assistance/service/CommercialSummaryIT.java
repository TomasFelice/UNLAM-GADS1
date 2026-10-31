package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(CommercialSummaryIT.Provider.class)
@Testcontainers
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CommercialSummaryIT {
    @Container static PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("DB_URL",postgres::getJdbcUrl);r.add("DB_USERNAME",postgres::getUsername);r.add("DB_PASSWORD",postgres::getPassword);
        r.add("spring.jpa.properties.hibernate.session_factory.statement_inspector",()->SqlCapture.class.getName());
    }
    static final AtomicReference<String> payload=new AtomicReference<>();
    static final AtomicInteger status=new AtomicInteger(200),calls=new AtomicInteger();
    static final AtomicReference<Runnable> during=new AtomicReference<>(()->{});
    static final AtomicReference<Runnable> afterOpportunity=new AtomicReference<>(()->{});
    public static class SqlCapture implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final List<String> sql=Collections.synchronizedList(new ArrayList<>());
        @Override public String inspect(String statement){sql.add(statement);return statement;}
    }
    static final MockEnvironment config=new MockEnvironment().withProperty("AI_SUMMARY_ENABLED","true").withProperty("GEMINI_API_KEY","fictitious-test-secret");
    static HttpServer server;
    @TestConfiguration static class Provider {
        @Bean @Primary SummarySettings testSummarySettings(){return new SummarySettings(config);}
        @Bean @Primary SummaryAdmission testAdmission(SummarySettings s,SummaryTime t) {
            // La admisión concurrente es real. Las cuotas se cubren con reloj en unitarias.
            return new SummaryAdmission(s,t){@Override public void countAttempt(){}};
        }
        @Bean @Primary SummaryGenerator testGenerator(SummarySettings s,JsonMapper json) throws Exception {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/models/",exchange->{
                calls.incrementAndGet();payload.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                during.get().run();
                String text="{\"clientNeeds\":[],\"currentSituation\":[{\"text\":\"Estado registrado\",\"sourceIds\":[\"O\"]}],\"agreementsAndObjections\":[],\"unconfirmedInformation\":[]}";
                String body=status.get()==200?json.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",text))))))):"upstream fictitious secret should never escape";
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status.get(),bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
            });server.start();
            var client=new GeminiSummaryClient(s,json,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/models/"));
            return (model,context,timeout)->{
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                return client.generate(model,context,timeout);
            };
        }
    }
    @Autowired TestRestTemplate http;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.ztech.crm.opportunities.service.OpportunitySummaryReader opportunityReader;
    @BeforeEach void reset(){status.set(200);calls.set(0);during.set(()->{});afterOpportunity.set(()->{});payload.set(null);config.setProperty("AI_SUMMARY_ENABLED","true");config.setProperty("GEMINI_SUMMARY_MODEL","gemini-3.1-flash-lite");
        org.mockito.Mockito.doAnswer(call->{var result=call.callRealMethod();afterOpportunity.getAndSet(()->{}).run();return result;}).when(opportunityReader).read(org.mockito.ArgumentMatchers.anyLong());
    }
    @AfterAll static void stop(){if(server!=null)server.stop(0);}
    @Test void persistsAcrossRequestsAndRolesRegeneratingOnlyForChangedContext() {
        var a=fixture().first();var h=login(a.admin());var url=path(a.ownOpportunityId());
        var first=send(url,null,h,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM commercial_summaries WHERE tenant_id=? AND opportunity_id=?",Long.class,a.id(),a.ownOpportunityId())).isEqualTo(1);
        status.set(503); // La lectura persistida no necesita un proveedor disponible.
        assertThat(send(url,null,login(a.seller()),200)).isEqualTo(first);
        assertThat(calls.get()).isEqualTo(1);
        jdbc.update("UPDATE opportunities SET notes='Nuevo acuerdo persistido' WHERE id=?",a.ownOpportunityId());
        send(url,null,h,503);
        assertThat(jdbc.queryForObject("SELECT response_json FROM commercial_summaries WHERE tenant_id=? AND opportunity_id=?",String.class,a.id(),a.ownOpportunityId())).contains((String)first.get("generatedAt"));
        status.set(200);
        var changed=send(url,null,h,200);
        assertThat(changed.get("generatedAt")).isNotEqualTo(first.get("generatedAt"));
        int previous=calls.get();
        assertThat(send(url,null,h,200)).isEqualTo(changed);assertThat(calls.get()).isEqualTo(previous);
        // Una actividad invalida aunque no cambie la versión de oportunidad.
        jdbc.update("INSERT INTO activities (tenant_id,type_id,occurred_at,opportunity_id,description,result,created_by) SELECT ?,id,now(),?,'Nuevo hecho','Confirmado',? FROM activity_types WHERE tenant_id=? LIMIT 1",a.id(),a.ownOpportunityId(),a.seller().id(),a.id());
        send(url,null,h,200);assertThat(calls.get()).isEqualTo(previous+1);
        jdbc.update("UPDATE stages SET name='Etapa renombrada' WHERE id=?",a.stageId());
        send(url,null,h,200);assertThat(calls.get()).isEqualTo(previous+2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM commercial_summaries WHERE tenant_id=? AND opportunity_id=?",Long.class,a.id(),a.ownOpportunityId())).isEqualTo(1);
    }
    @Test void threeRolesAreAuthorizedForeignScopeAndAnonymousNeverCallProviderAndNoWritesOccur() {
        var f=fixture();var a=f.first();var path=path(a.ownOpportunityId());var before=state(a);
        for(var account:List.of(a.admin(),a.manager(),a.seller())) {
            var result=send(path,null,login(account),200);assertThat(result).containsKeys("coverage","sources","sections");
        }
        assertThat(state(a)).isEqualTo(before);int previous=calls.get();
        send(path,null,login(a.otherSeller()),404);send(path,null,login(f.second().admin()),404);
        send(path,null,new HttpHeaders(),401);assertThat(calls.get()).isEqualTo(previous);
        send(path,"{}",login(a.admin()),400);send(path+"?model=unknown",null,login(a.admin()),400);
        assertThat(calls.get()).isEqualTo(previous);
    }
    @Test void sourcesUseRecentFiftyWithIdTieBreakAndExcludeSharedCustomerAndForeignSources() {
        var f=fixture();var a=f.first();
        long type=jdbc.queryForObject("SELECT id FROM activity_types WHERE tenant_id=? LIMIT 1",Long.class,a.id());
        for(int i=0;i<55;i++)jdbc.update("INSERT INTO activities (tenant_id,type_id,occurred_at,company_id,opportunity_id,description,result,created_by) VALUES (?,?,'2026-10-02T12:00:00Z',?,?,?, 'Acuerdo ficticio',?)",a.id(),type,a.companyId(),a.ownOpportunityId(),"Fuente "+i,a.seller().id());
        jdbc.update("UPDATE activities SET description='EXCLUDED_SHARED_CUSTOMER' WHERE opportunity_id=?",a.otherOpportunityId());
        jdbc.update("INSERT INTO activities (tenant_id,type_id,occurred_at,company_id,description,result,created_by) VALUES (?,?,'2026-10-03T12:00:00Z',?,'EXCLUDED_GENERAL','x',?)",a.id(),type,a.companyId(),a.seller().id());
        for(int i=0;i<55;i++)jdbc.update("INSERT INTO stage_history (tenant_id,opportunity_id,from_stage_id,to_stage_id,changed_by,changed_at,note,event_type,snapshot_complete,destination_status) VALUES (?,?,?,?,?,'2026-10-02T12:00:00Z',?,'STAGE_CHANGED',true,'ABIERTA')",a.id(),a.ownOpportunityId(),a.stageId(),a.stageId(),a.seller().id(),"Paso "+i);
        var result=send(path(a.ownOpportunityId()),null,login(a.seller()),200);
        var coverage=(Map<?,?>)result.get("coverage");assertThat(((Number)coverage.get("totalActivities")).intValue()).isEqualTo(56);
        assertThat(((Number)coverage.get("includedActivities")).intValue()).isLessThanOrEqualTo(50);
        assertThat(coverage.get("partial")).isEqualTo(true);
        assertThat(payload.get()).contains("Fuente 54","Paso 54").doesNotContain("EXCLUDED_SHARED_CUSTOMER","EXCLUDED_GENERAL","Fuente 0\"","tenantId","fictitious-test-secret");
        assertThat(payload.get()).doesNotContain(f.second().admin().email());
        assertThat(SqlCapture.sql.stream().filter(s->s.contains("from activities")&&s.contains("order by")).toList()).anyMatch(s->s.contains("fetch first"));
        assertThat(SqlCapture.sql.stream().filter(s->s.contains("from stage_history")&&s.contains("order by")).toList()).allMatch(s->s.contains("fetch first"));
    }
    @Test void snapshotIsRepeatableReadAcrossModulesAndConcurrentInsertIsDetectedOnRevalidation() {
        var a=fixture().first();var h=login(a.admin());
        afterOpportunity.set(()->{
            var writer=Thread.ofVirtual().start(()->jdbc.update("INSERT INTO activities (tenant_id,type_id,occurred_at,opportunity_id,description,result,created_by) SELECT ?,id,now(),?,'CONCURRENT_AFTER_SNAPSHOT','Confirmado',? FROM activity_types WHERE tenant_id=? LIMIT 1",a.id(),a.ownOpportunityId(),a.seller().id(),a.id()));
            try{writer.join();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
        });
        assertThat(send(path(a.ownOpportunityId()),null,h,409)).containsEntry("code","AI_CONTEXT_CHANGED");
        assertThat(payload.get()).doesNotContain("CONCURRENT_AFTER_SNAPSHOT");
    }
    @Test void changesDuringProviderDiscardResultAndReleaseDatabaseTransaction() {
        var a=fixture().first();var h=login(a.seller());
        during.set(()->jdbc.update("UPDATE opportunities SET sales_rep_id=?,version=version+1 WHERE id=?",a.otherSeller().id(),a.ownOpportunityId()));
        send(path(a.ownOpportunityId()),null,h,404);
        jdbc.update("UPDATE opportunities SET sales_rep_id=? WHERE id=?",a.seller().id(),a.ownOpportunityId());
        long nextStage=jdbc.queryForObject("INSERT INTO stages (tenant_id,pipeline_id,name,position,kind,active) VALUES (?,?,'Nueva etapa ficticia',10,'OPEN',true) RETURNING id",Long.class,a.id(),a.pipelineId());
        during.set(()->jdbc.update("UPDATE opportunities SET stage_id=?,version=version+1 WHERE id=?",nextStage,a.ownOpportunityId()));
        assertThat(send(path(a.ownOpportunityId()),null,h,409)).containsEntry("code","AI_CONTEXT_CHANGED");
        during.set(()->jdbc.update("INSERT INTO activities (tenant_id,type_id,occurred_at,opportunity_id,description,result,created_by) SELECT ?,id,now(),?,'Nueva actividad ficticia','Confirmado',? FROM activity_types WHERE tenant_id=? LIMIT 1",a.id(),a.ownOpportunityId(),a.seller().id(),a.id()));
        assertThat(send(path(a.ownOpportunityId()),null,h,409)).containsEntry("code","AI_CONTEXT_CHANGED");
    }
    @Test void disabledInvalidConfigCredentialQuotaAndUnavailableAreIsolatedAndSanitized(org.springframework.boot.test.system.CapturedOutput output) {
        var a=fixture().first();var h=login(a.admin());var before=state(a);
        config.setProperty("AI_SUMMARY_ENABLED","false");assertThat(send(path(a.ownOpportunityId()),null,h,503)).containsEntry("code","AI_NOT_CONFIGURED");
        assertThat(http.getForEntity("/actuator/health",String.class).getStatusCode().value()).isEqualTo(200);assertThat(calls.get()).isZero();
        config.setProperty("AI_SUMMARY_ENABLED","true");config.setProperty("GEMINI_SUMMARY_MODEL","gemini-flash-latest");send(path(a.ownOpportunityId()),null,h,503);
        assertThat(http.getForEntity("/actuator/health",String.class).getStatusCode().value()).isEqualTo(200);assertThat(calls.get()).isZero();
        config.setProperty("GEMINI_SUMMARY_MODEL","gemini-3.1-flash-lite");
        status.set(401);assertThat(send(path(a.ownOpportunityId()),null,h,503)).containsEntry("code","AI_UNAVAILABLE");
        status.set(429);assertThat(send(path(a.ownOpportunityId()),null,h,429)).containsEntry("code","AI_QUOTA_EXCEEDED");
        status.set(503);assertThat(send(path(a.ownOpportunityId()),null,h,503)).containsEntry("code","AI_UNAVAILABLE");
        assertThat(state(a)).isEqualTo(before);
        assertThat(output.getAll()).contains("outcome=AI_UNAVAILABLE","promptVersion=", "attempts=")
                .doesNotContain("upstream fictitious secret", "fictitious-test-secret", "Llamada realizada");
    }
    private List<?> state(CommercialFixtures.Organization a) {
        return List.of(jdbc.queryForList("SELECT * FROM opportunities WHERE id=?",a.ownOpportunityId()),
                jdbc.queryForList("SELECT * FROM activities WHERE opportunity_id=? ORDER BY id",a.ownOpportunityId()),
                jdbc.queryForList("SELECT * FROM stage_history WHERE opportunity_id=? ORDER BY id",a.ownOpportunityId()));
    }
    private CommercialFixtures.Scenario fixture(){return CommercialFixtures.create(jdbc,encoder,"summary."+UUID.randomUUID());}
    private String path(long id){return "/api/v1/opportunities/"+id+"/commercial-summary";}
    private HttpHeaders login(CommercialFixtures.Account a){var r=http.postForEntity("/api/v1/auth/login",new LoginRequest(a.email(),CommercialFixtures.PASSWORD),LoginResponse.class);assertThat(r.getStatusCode().value()).isEqualTo(200);var h=new HttpHeaders();h.setBearerAuth(r.getBody().accessToken());return h;}
    private Map<String,Object> send(String path,Object body,HttpHeaders h,int expected){var r=http.exchange(path,HttpMethod.POST,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});assertThat(r.getStatusCode().value()).as(path+": "+r.getBody()).isEqualTo(expected);assertThat(r.getBody().toString()).doesNotContain("upstream fictitious secret");return r.getBody();}
}
