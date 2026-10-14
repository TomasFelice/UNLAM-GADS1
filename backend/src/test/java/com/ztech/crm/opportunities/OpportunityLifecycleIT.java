package com.ztech.crm.opportunities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.service.OpportunityAvailabilityService;
import com.ztech.crm.support.CommercialFixtures;
import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class OpportunityLifecycleIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("DB_URL",postgres::getJdbcUrl);r.add("DB_USERNAME",postgres::getUsername);r.add("DB_PASSWORD",postgres::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean OpportunityAvailabilityService availability;

    @Test void assignsWithActorDateAndChangesSellerScopeWithoutLosingActivities() {
        var a=fixture("assign").first();var admin=login(a.admin());long id=a.ownOpportunityId();
        var oldActivities=jdbc.queryForList("SELECT * FROM activities WHERE opportunity_id=? ORDER BY id",id);
        send(id+"/assign",Map.of("salesRepId",a.otherSeller().id(),"version",0),login(a.seller()),403);
        send(id+"/assign",Map.of("salesRepId",a.otherSeller().id(),"version",0),admin,200);
        get("opportunities/"+id,login(a.seller()),404);get("opportunities/"+id,login(a.otherSeller()),200);
        assertThat(jdbc.queryForObject("SELECT updated_by FROM opportunities WHERE id=?",Long.class,id)).isEqualTo(a.admin().id());
        assertThat(jdbc.queryForObject("SELECT updated_at FROM opportunities WHERE id=?",java.sql.Timestamp.class,id)).isNotNull();
        assertThat(jdbc.queryForList("SELECT * FROM activities WHERE opportunity_id=? ORDER BY id",id)).isEqualTo(oldActivities);
        send(id+"/assign",Map.of("salesRepId",a.seller().id(),"version",1),login(a.manager()),200);
        jdbc.update("UPDATE users SET active=false WHERE id=?",a.otherSeller().id());
        send(id+"/assign",Map.of("salesRepId",a.otherSeller().id(),"version",2),admin,422);
    }

    @Test void validatesRequiredDataKindsAndForeignIdsForAllRolesWithoutPartialWrites() {
        var f=fixture("negative");var a=f.first();var h=login(a.admin());long id=a.ownOpportunityId();
        send(id+"/win",Map.of("stageId",stage(a,"WON"),"version",0),h,400);
        send(id+"/win",Map.of("stageId",stage(a,"WON"),"finalValue",-1,"version",0),h,400);
        send(id+"/win",Map.of("stageId",stage(a,"WON"),"finalValue",10),h,400);
        send(id+"/win",win(a,a.stageId(),0),h,409);
        send(id+"/lose",Map.of("stageId",stage(a,"LOST"),"version",0),h,400);
        send(id+"/lose",Map.of("stageId",stage(a,"WON"),"lossReasonId",reason(a),"version",0),h,409);
        for (var account:List.of(a.admin(),a.manager(),a.seller())) {
            var role=login(account);
            get("opportunities/"+f.second().ownOpportunityId()+"/availability",role,404);
            send(f.second().ownOpportunityId()+"/win",win(a,stage(a,"WON"),0),role,404);
            send(id+"/win",win(a,stage(f.second(),"WON"),0),role,404);
            send(id+"/lose",Map.of("stageId",stage(a,"LOST"),"lossReasonId",reason(f.second()),"version",0),role,404);
        }
        send(id+"/assign",Map.of("salesRepId",f.second().seller().id(),"version",0),h,404);
        send(a.otherOpportunityId()+"/win",win(a,stage(a,"WON"),0),login(a.seller()),404);
        assertThat(detail(id,h)).containsEntry("status","ABIERTA").containsEntry("version",0);
        assertThat(history(id,h)).hasSize(1);
    }

    @Test void winReopenLoseReconstructsBothClosuresAndReleasesReservation() {
        var a=fixture("cycles").first();var h=login(a.admin());long id=a.ownOpportunityId();
        Instant before=Instant.now();send(id+"/win",win(a,stage(a,"WON"),0),login(a.seller()),200);
        var won=detail(id,h);assertThat(won).containsEntry("status","GANADA").containsEntry("version",1);
        assertThat(Instant.parse((String)won.get("closedAt"))).isBetween(before,Instant.now());
        assertThat(get("opportunities/"+a.otherOpportunityId()+"/availability",h,200)).containsEntry("rangeFree",false);
        send(id+"/assign",Map.of("salesRepId",a.otherSeller().id(),"version",1),h,409);
        send(id+"/stage",Map.of("stageId",a.stageId(),"version",1),h,409);
        send(id+"/win",win(a,stage(a,"WON"),1),h,409);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Revisar propuesta","version",1),login(a.seller()),403);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note"," ","version",1),h,400);
        send(id+"/reopen",Map.of("stageId",stage(a,"LOST"),"note","Nueva negociación","version",1),h,409);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Nueva negociación","version",1),login(a.manager()),200);
        assertThat(detail(id,h)).containsEntry("status","ABIERTA").containsEntry("closedAt",null).containsEntry("finalValue",null).containsEntry("lossReasonId",null).containsEntry("version",2);
        assertThat(get("opportunities/"+a.otherOpportunityId()+"/availability",h,200)).containsEntry("rangeFree",true);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Ya abierta","version",2),h,409);
        jdbc.update("UPDATE loss_reasons SET active=false WHERE id=?",reason(a));
        send(id+"/lose",Map.of("stageId",stage(a,"LOST"),"lossReasonId",reason(a),"version",2),h,422);
        jdbc.update("UPDATE loss_reasons SET active=true WHERE id=?",reason(a));
        send(id+"/lose",Map.of("stageId",stage(a,"LOST"),"lossReasonId",reason(a),"note","Sin presupuesto","version",2),h,200);
        jdbc.update("UPDATE loss_reasons SET name='Nombre posterior',active=false WHERE id=?",reason(a));
        var events=history(id,h);assertThat(events).hasSize(4);
        assertThat(events.get(0)).containsEntry("eventType","LEGACY").containsEntry("snapshotComplete",false);
        assertThat(events.get(1)).containsEntry("eventType","WON").containsEntry("destinationStatus","GANADA").containsEntry("finalValue",1234.5).containsEntry("closedAt",won.get("closedAt"));
        assertThat(events.get(2)).containsEntry("eventType","REOPENED").containsEntry("note","Nueva negociación");
        assertThat(events.get(3)).containsEntry("eventType","LOST").containsEntry("lossReasonName","Presupuesto").containsEntry("lossReasonId",(int)reason(a));
        assertThat(get("opportunities/"+a.otherOpportunityId()+"/availability",h,200)).containsEntry("available",true);
    }

    @Test void revalidatesVenueStateAndCapacityAndRejectsInactiveReopeningPipeline() {
        var a=fixture("revalidate").first();var h=login(a.admin());long id=a.ownOpportunityId();
        jdbc.update("UPDATE venues SET capacity=10 WHERE id=?",a.venueId());
        send(id+"/win",win(a,stage(a,"WON"),0),h,422);
        jdbc.update("UPDATE venues SET capacity=80,status='MANTENIMIENTO' WHERE id=?",a.venueId());
        send(id+"/win",win(a,stage(a,"WON"),0),h,422);
        jdbc.update("UPDATE venues SET status='DISPONIBLE' WHERE id=?",a.venueId());
        jdbc.update("UPDATE pipelines SET is_default=false,active=false WHERE id=?",a.pipelineId());
        send(id+"/win",win(a,stage(a,"WON"),0),h,200);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Intento","version",1),h,409);
        assertThat(detail(id,h)).containsEntry("status","GANADA");
    }

    @Test void historyInsertFailureRollsBackClosureAndVersionThroughRealHttp() {
        var a=fixture("rollback").first();var h=login(a.admin());long id=a.ownOpportunityId();
        jdbc.execute("CREATE FUNCTION reject_closure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.opportunity_id="+id+" THEN RAISE EXCEPTION 'fixture failure'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER reject_closure BEFORE INSERT ON stage_history FOR EACH ROW EXECUTE FUNCTION reject_closure()");
        try {
            send(id+"/win",win(a,stage(a,"WON"),0),h,500);
            assertThat(detail(id,h)).containsEntry("status","ABIERTA").containsEntry("version",0).containsEntry("closedAt",null);
            assertThat(history(id,h)).hasSize(1);
            assertThat(get("opportunities/"+a.otherOpportunityId()+"/availability",h,200)).containsEntry("available",true);
        } finally { jdbc.execute("DROP TRIGGER reject_closure ON stage_history");jdbc.execute("DROP FUNCTION reject_closure()"); }
    }

    @Test void respectsContiguousRangesOtherVenuesTenantsAndConflictsAcrossPipelines() {
        var f=fixture("ranges");var a=f.first();var h=login(a.admin());long id=a.ownOpportunityId();
        send(id+"/win",win(a,stage(a,"WON"),0),h,200);
        var otherPipeline=jdbc.queryForObject("INSERT INTO pipelines(tenant_id,name,active,is_default) VALUES (?,'Segundo',true,false) RETURNING id",Long.class,a.id());
        var otherStage=jdbc.queryForObject("INSERT INTO stages(tenant_id,pipeline_id,name,position,kind) VALUES (?,?,'Ganada',1,'WON') RETURNING id",Long.class,a.id(),otherPipeline);
        jdbc.update("UPDATE opportunities SET pipeline_id=?,stage_id=? WHERE id=?",otherPipeline,otherStage,a.otherOpportunityId());
        assertThat(send(a.otherOpportunityId()+"/win",win(a,otherStage,0),h,409)).containsEntry("code","VENUE_ALREADY_BOOKED");
        jdbc.update("UPDATE opportunities SET event_start='2030-01-01T16:00:00Z',event_end='2030-01-01T18:00:00Z' WHERE id=?",a.otherOpportunityId());
        send(a.otherOpportunityId()+"/win",win(a,otherStage,0),h,200);
        send(f.second().ownOpportunityId()+"/win",win(f.second(),stage(f.second(),"WON"),0),login(f.second().admin()),200);
        long extra=jdbc.queryForObject("INSERT INTO venues(tenant_id,name,capacity,status) VALUES (?,'Otro',80,'DISPONIBLE') RETURNING id",Long.class,a.id());
        jdbc.update("UPDATE opportunities SET venue_id=? WHERE id=?",extra,a.otherOpportunityId());
        assertThat(get("opportunities/"+a.otherOpportunityId()+"/availability",h,200)).containsEntry("rangeFree",true);
    }

    @org.junit.jupiter.api.RepeatedTest(3) void simultaneousDifferentOpportunitiesPassBothPrechecksButGistAllowsExactlyOne() throws Exception {
        var a=fixture("race."+UUID.randomUUID()).first();var h=login(a.admin());var barrier=new CyclicBarrier(2);
        doAnswer(invocation->{invocation.callRealMethod();barrier.await(10,TimeUnit.SECONDS);return null;}).when(org.springframework.test.util.AopTestUtils.<OpportunityAvailabilityService>getUltimateTargetObject(availability)).assertConfirmable(any(Opportunity.class));
        List<ResponseEntity<Map<String,Object>>> results;
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->post(a.ownOpportunityId()+"/win",win(a,stage(a,"WON"),0),h));
            var second=pool.submit(()->post(a.otherOpportunityId()+"/win",win(a,stage(a,"WON"),0),h));
            results=List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
        }
        assertThat(results).extracting(r->r.getStatusCode().value()).containsExactlyInAnyOrder(200,409);
        assertThat(results.stream().filter(r->r.getStatusCode().value()==409).findFirst().orElseThrow().getBody().get("code"))
                .isIn("VENUE_ALREADY_BOOKED", "CONCURRENT_WRITE_CONFLICT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunities WHERE tenant_id=? AND status='GANADA'",Integer.class,a.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stage_history WHERE tenant_id=? AND event_type='WON'",Integer.class,a.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunities WHERE tenant_id=? AND status='ABIERTA' AND version=0",Integer.class,a.id())).isEqualTo(1);
    }

    @Test void concurrentSameVersionAndLateRequestsNeverOverwriteOrDuplicateHistory() throws Exception {
        var a=fixture("version").first();var h=login(a.admin());long id=a.ownOpportunityId();var barrier=new CyclicBarrier(2);
        doAnswer(invocation->{invocation.callRealMethod();barrier.await(10,TimeUnit.SECONDS);return null;}).when(org.springframework.test.util.AopTestUtils.<OpportunityAvailabilityService>getUltimateTargetObject(availability)).assertConfirmable(any(Opportunity.class));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->post(id+"/win",win(a,stage(a,"WON"),0),h));
            var second=pool.submit(()->post(id+"/win",win(a,stage(a,"WON"),0),h));
            var results=List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
            assertThat(results).extracting(r->r.getStatusCode().value()).containsExactlyInAnyOrder(200,409);
            assertThat(results.stream().filter(r->r.getStatusCode().value()==409).findFirst().orElseThrow().getBody()).containsEntry("code","OPPORTUNITY_VERSION_CONFLICT");
        }
        assertThat(history(id,h)).hasSize(2);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Tardía","version",0),h,409);
        send(id+"/reopen",Map.of("stageId",a.stageId(),"note","Vigente","version",1),h,200);
        assertThat(send(id+"/assign",Map.of("salesRepId",a.otherSeller().id(),"version",1),h,409)).containsEntry("code","OPPORTUNITY_VERSION_CONFLICT");
        assertThat(detail(id,h)).containsEntry("version",2).containsEntry("salesRepId",(int)a.seller().id());
        assertThat(history(id,h)).hasSize(3);
    }

    @Test void genericEditRequiresReadVersionAndNeverOverwritesALaterWriteOrAClosedOpportunity() {
        var a=fixture("edit.version").first();var h=login(a.admin());long id=a.ownOpportunityId();
        var body=new HashMap<String,Object>(Map.of("title","Edición leída","companyId",a.companyId(),"contactId",a.contactId(),
                "venueId",a.venueId(),"eventTypeId",a.eventTypeId(),"eventStart","2030-01-01T12:00:00Z",
                "eventEnd","2030-01-01T16:00:00Z","attendeeCount",20));
        assertThat(put(id,body,h).getStatusCode().value()).isEqualTo(400);
        body.put("version",0);
        var success=put(id,body,h);assertThat(success.getStatusCode().value()).isEqualTo(200);
        assertThat(success.getBody()).containsEntry("version",1);
        body.put("title","Petición tardía");
        var stale=put(id,body,h);assertThat(stale.getStatusCode().value()).isEqualTo(409);
        assertThat(stale.getBody()).containsEntry("code","OPPORTUNITY_VERSION_CONFLICT");
        assertThat(detail(id,h)).containsEntry("title","Edición leída").containsEntry("version",1);
        send(id+"/lose",Map.of("stageId",stage(a,"LOST"),"lossReasonId",reason(a),"version",1),login(a.manager()),200);
        body.put("version",2);assertThat(put(id,body,h).getStatusCode().value()).isEqualTo(409);
        assertThat(detail(id,h)).containsEntry("status","PERDIDA").containsEntry("title","Edición leída");
    }
    private ResponseEntity<Map<String,Object>> put(long id,Object body,HttpHeaders h){return http.exchange("/api/v1/opportunities/"+id,HttpMethod.PUT,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});}

    private CommercialFixtures.Scenario fixture(String key){return CommercialFixtures.create(jdbc,encoder,"lifecycle."+key);}
    private long stage(CommercialFixtures.Organization a,String kind){return jdbc.queryForObject("SELECT id FROM stages WHERE tenant_id=? AND pipeline_id=? AND kind=?",Long.class,a.id(),a.pipelineId(),kind);}
    private long reason(CommercialFixtures.Organization a){return jdbc.queryForObject("SELECT id FROM loss_reasons WHERE tenant_id=? ORDER BY id LIMIT 1",Long.class,a.id());}
    private Map<String,Object> win(CommercialFixtures.Organization a,long stage,long version){return Map.of("stageId",stage,"finalValue",1234.50,"note","Confirmada","version",version);}
    private Map<String,Object> detail(long id,HttpHeaders h){return get("opportunities/"+id,h,200);}
    private List<Map<String,Object>> history(long id,HttpHeaders h){return http.exchange("/api/v1/opportunities/"+id+"/stage-history",HttpMethod.GET,new HttpEntity<>(h),new ParameterizedTypeReference<List<Map<String,Object>>>(){}).getBody();}
    private HttpHeaders login(CommercialFixtures.Account account){var r=http.postForEntity("/api/v1/auth/login",new LoginRequest(account.email(),CommercialFixtures.PASSWORD),LoginResponse.class);assertThat(r.getStatusCode().value()).isEqualTo(200);var h=new HttpHeaders();h.setBearerAuth(r.getBody().accessToken());return h;}
    private Map<String,Object> get(String path,HttpHeaders h,int status){var r=http.exchange("/api/v1/"+path,HttpMethod.GET,new HttpEntity<>(h),new ParameterizedTypeReference<Map<String,Object>>(){});assertThat(r.getStatusCode().value()).as(r.getBody().toString()).isEqualTo(status);return r.getBody();}
    private ResponseEntity<Map<String,Object>> post(String path,Object body,HttpHeaders h){return http.exchange("/api/v1/opportunities/"+path,HttpMethod.POST,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});}
    private Map<String,Object> send(String path,Object body,HttpHeaders h,int status){var r=post(path,body,h);assertThat(r.getStatusCode().value()).as(path+": "+r.getBody()).isEqualTo(status);return r.getBody();}
}
