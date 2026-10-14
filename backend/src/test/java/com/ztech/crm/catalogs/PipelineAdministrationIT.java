package com.ztech.crm.catalogs;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
class PipelineAdministrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("DB_URL", postgres::getJdbcUrl); r.add("DB_USERNAME", postgres::getUsername); r.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void templatesAreIndependentAndActivationAndDefaultAreValidated() {
        var a = fixture("templates").first(); var h = login(a.admin());
        assertThat(array("pipeline-templates", h)).extracting(v -> v.get("key")).containsExactly("corporate-events", "brief-hiring");
        long empty = create("Vacío", null, h);
        assertThat(stages(empty, h)).isEmpty();
        send("pipelines/" + empty, HttpMethod.PUT, entry("Vacío", true), h, 409);
        send("pipelines/" + empty + "/default", HttpMethod.POST, null, h, 409);
        long first = create("Breve", "brief-hiring", h), second = create("Otra copia", "brief-hiring", h);
        assertThat(stages(first, h)).hasSize(4);
        assertThat(stages(first, h)).extracting(v -> v.get("id")).doesNotContainAnyElementsOf(stages(second, h).stream().map(v -> v.get("id")).toList());
        long stage = number(stages(first, h).getFirst(), "id");
        send("stages/" + stage, HttpMethod.PUT, stage(first, "Consulta renombrada", "OPEN", true), h, 200);
        assertThat(stages(second, h).getFirst()).containsEntry("name", "Consulta recibida");
        for (String kind : List.of("OPEN", "WON", "LOST")) send("stages", HttpMethod.POST, stage(empty, kind, kind, true), h, 201);
        send("pipelines/" + empty, HttpMethod.PUT, entry("Ya completo", true), h, 200);
        send("pipelines/" + empty + "/default", HttpMethod.POST, null, h, 200);
        assertThat(send("pipelines/default", HttpMethod.GET, null, h, 200)).containsEntry("id", (int) empty);
        send("pipelines/" + empty, HttpMethod.PUT, entry("Ya completo", false), h, 409);
        send("pipelines/" + a.pipelineId(), HttpMethod.PUT, entry("Proceso histórico", false), h, 200);
        assertThat(send("pipelines/" + a.pipelineId(), HttpMethod.GET, null, h, 200)).containsEntry("active", false);
        send("pipelines", HttpMethod.POST, Map.of("name", "Ya completo"), h, 409);
        send("pipelines", HttpMethod.POST, Map.of("name", "Plantilla inválida", "templateKey", "unknown"), h, 404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pipelines WHERE tenant_id=?", Integer.class, a.id())).isEqualTo(4);
    }

    @Test void scopesReadAndWriteForThreeRolesAndTwoTenants() {
        var s=fixture("scope"); var a=s.first(); long local=a.pipelineId(), foreign=s.second().pipelineId();
        for(var account:List.of(a.admin(),a.manager(),a.seller())) {
            var h=login(account); boolean admin=account.equals(a.admin());
            send("pipelines/"+local,HttpMethod.GET,null,h,200);
            send("pipelines/"+foreign,HttpMethod.GET,null,h,404);
            send("opportunities/board?pipelineId="+foreign,HttpMethod.GET,null,h,404);
            send("opportunities?pipelineId="+foreign,HttpMethod.GET,null,h,404);
            assertThat((List<?>)send("pipelines",HttpMethod.GET,null,h,200).get("content")).hasSize(1);
            assertThat(array("stages?pipelineId="+local,h)).hasSize(3);
            send("stages?pipelineId="+foreign,HttpMethod.GET,null,h,404);
            send("stages/"+s.second().stageId(),HttpMethod.GET,null,h,404);
            send("pipelines/"+foreign,HttpMethod.PUT,entry("Inválido",false),h,admin?404:403);
            send("pipelines/"+foreign+"/default",HttpMethod.POST,null,h,admin?404:403);
            send("pipelines/"+foreign+"/stages/reorder",HttpMethod.POST,Map.of("stageIds",List.of()),h,admin?404:403);
            send("stages/"+s.second().stageId(),HttpMethod.PUT,stage(foreign,"Inválido","OPEN",true),h,admin?404:403);
            if(!admin) {
                send("pipelines",HttpMethod.POST,Map.of("name","Inválido"),h,403);
                send("pipelines/"+local,HttpMethod.PUT,entry("Inválido",true),h,403);
                send("stages",HttpMethod.POST,stage(local,"Inválido","OPEN",true),h,403);
            }
        }
        var h=login(a.admin());
        for(String query:List.of("page=-1","size=101","sort=tenantId,asc","active=DESCONOCIDO"))send("pipelines?"+query,HttpMethod.GET,null,h,400);
        for(int i=0;i<22;i++)create("Proceso "+i,null,h);
        assertThat(send("pipelines?page=1",HttpMethod.GET,null,h,200)).containsEntry("totalPages",2);
        assertThat(send("pipelines?q=21&active=false",HttpMethod.GET,null,h,200)).containsEntry("totalElements",1);
    }

    @Test void usedStageKeepsKindPipelineAndHistoryAndActivePipelineKeepsMinimumKinds() {
        var a=fixture("stages").first(); var h=login(a.admin());
        long other=create("Otro proceso",null,h);
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(a.pipelineId(),"Cierre incompatible","WON",true),h,409);
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(other,"Movimiento","OPEN",true),h,409);
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(a.pipelineId(),"Sin abierta","OPEN",false),h,409);
        long replacement=number(send("stages",HttpMethod.POST,stage(a.pipelineId(),"Nueva entrada","OPEN",true),h,201),"id");
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(a.pipelineId(),"Consulta antigua","OPEN",false),h,200);
        assertThat(send("opportunities/"+a.ownOpportunityId(),HttpMethod.GET,null,h,200).get("stage").toString()).contains("Consulta antigua","active=false");
        assertThat(jdbc.queryForObject("SELECT to_stage_id FROM stage_history WHERE opportunity_id=?",Long.class,a.ownOpportunityId())).isEqualTo(a.stageId());
        // A stage referenced only by history is still immutable.
        jdbc.update("UPDATE opportunities SET stage_id=? WHERE tenant_id=?",replacement,a.id());
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(a.pipelineId(),"Historia","LOST",false),h,409);
        long lost=stages(a.pipelineId(),h).stream().filter(v->v.get("kind").equals("LOST")).mapToLong(v->number(v,"id")).findFirst().orElseThrow();
        send("stages/"+lost,HttpMethod.PUT,stage(a.pipelineId(),"Perdida","LOST",false),h,409);
        long unused=number(send("stages",HttpMethod.POST,stage(other,"Sin usar","OPEN",true),h,201),"id");
        send("stages/"+unused,HttpMethod.PUT,stage(other,"Reconfigurada","WON",false),h,200);
    }

    @Test void reorderIsAtomicAndRejectsIncompleteDuplicateAndForeignSets() {
        var s=fixture("order");var a=s.first();var h=login(a.admin());
        var ids=ids(a.pipelineId(),h);var reversed=new ArrayList<>(ids);Collections.reverse(reversed);
        reorder(a.pipelineId(),reversed,h,200);
        assertThat(ids(a.pipelineId(),h)).isEqualTo(reversed);
        for(var invalid:List.of(List.of(ids.getFirst()),List.of(ids.getFirst(),ids.getFirst(),ids.getLast()),List.of(ids.getFirst(),ids.get(1),s.second().stageId()))) {
            reorder(a.pipelineId(),invalid,h,422);assertThat(ids(a.pipelineId(),h)).isEqualTo(reversed);
        }
        assertThat(jdbc.queryForList("SELECT position FROM stages WHERE pipeline_id=? ORDER BY position",Integer.class,a.pipelineId())).containsExactly(1,2,3);
    }

    @Test void concurrentOrdersAndDefaultsSerializeWithoutPartialConfiguration() throws Exception {
        var a=fixture("concurrent").first();var h=login(a.admin());
        long other=create("Alternativo","brief-hiring",h);send("pipelines/"+other,HttpMethod.PUT,entry("Alternativo",true),h,200);
        var ids=ids(a.pipelineId(),h);var reversed=new ArrayList<>(ids);Collections.reverse(reversed);
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{barrier.await();reorder(a.pipelineId(),ids,h,200);send("pipelines/"+other+"/default",HttpMethod.POST,null,h,200);return true;});
            var second=pool.submit(()->{barrier.await();reorder(a.pipelineId(),reversed,h,200);send("pipelines/"+a.pipelineId()+"/default",HttpMethod.POST,null,h,200);return true;});
            assertThat(first.get()).isTrue();assertThat(second.get()).isTrue();
        }
        assertThat(ids(a.pipelineId(),h)).isIn(ids,reversed);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pipelines WHERE tenant_id=? AND is_default AND active",Integer.class,a.id())).isEqualTo(1);
    }

    @Test void opportunitiesKeepTheirPipelineAndBoardUsesKindAndHistoricalStages() {
        var a=fixture("commercial").first();var h=login(a.admin());
        long other=create("Contrataciones","brief-hiring",h);send("pipelines/"+other,HttpMethod.PUT,entry("Contrataciones",true),h,200);
        long otherOpen=number(stages(other,h).getFirst(),"id");
        var body=opportunity(a);body.put("stageId",otherOpen);body.put("pipelineId",a.pipelineId());
        send("opportunities",HttpMethod.POST,body,h,409);
        body.put("pipelineId",other);long created=number(send("opportunities",HttpMethod.POST,body,h,201),"id");
        assertThat(send("opportunities/"+created,HttpMethod.GET,null,h,200)).containsEntry("pipelineId",(int)other);
        body.put("pipelineId",a.pipelineId());body.put("version",0);send("opportunities/"+created,HttpMethod.PUT,body,h,409);
        send("opportunities/"+a.ownOpportunityId()+"/stage",HttpMethod.POST,Map.of("stageId",otherOpen,"version",0),h,409);
        long replacement=number(send("stages",HttpMethod.POST,stage(a.pipelineId(),"Entrada","OPEN",true),h,201),"id");
        send("stages/"+a.stageId(),HttpMethod.PUT,stage(a.pipelineId(),"Sin palabra comercial","OPEN",false),h,200);
        send("pipelines/"+other+"/default",HttpMethod.POST,null,h,200);
        send("pipelines/"+a.pipelineId(),HttpMethod.PUT,entry("Histórico",false),h,200);
        var board=send("opportunities/board?pipelineId="+a.pipelineId(),HttpMethod.GET,null,login(a.seller()),200);
        assertThat(board.get("pipeline").toString()).contains("Histórico","active=false");
        var columns=(List<Map<String,Object>>)board.get("columns");
        var old=columns.stream().filter(c->number(c,"stageId")==a.stageId()).findFirst().orElseThrow();
        assertThat(old).containsEntry("stageName","Sin palabra comercial").containsEntry("kind","OPEN").containsEntry("active",false);
        assertThat((List<Map<String,Object>>)old.get("opportunities")).extracting(v->number(v,"id")).containsExactly(a.ownOpportunityId());
        assertThat(columns).extracting(v->v.get("kind")).contains("WON","LOST");
        long lost=columns.stream().filter(c->c.get("kind").equals("LOST")).mapToLong(c->number(c,"stageId")).findFirst().orElseThrow();
        jdbc.update("UPDATE opportunities SET stage_id=?,status='PERDIDA' WHERE id=?",lost,a.otherOpportunityId());
        var closedBoard=send("opportunities/board?pipelineId="+a.pipelineId()+"&status=PERDIDA",HttpMethod.GET,null,h,200);
        var closedColumns=(List<Map<String,Object>>)closedBoard.get("columns");
        assertThat((List<Map<String,Object>>)closedColumns.stream().filter(c->number(c,"stageId")==lost).findFirst().orElseThrow().get("opportunities")).extracting(v->number(v,"id")).containsExactly(a.otherOpportunityId());
        assertThat(send("opportunities?pipelineId="+other,HttpMethod.GET,null,h,200)).containsEntry("totalElements",1);
        body.put("pipelineId",a.pipelineId());body.put("stageId",replacement);send("opportunities",HttpMethod.POST,body,h,409);
        assertThat(send("opportunities/board",HttpMethod.GET,null,h,200).get("pipeline").toString()).contains("Contrataciones");
    }

    private CommercialFixtures.Scenario fixture(String key){return CommercialFixtures.create(jdbc,encoder,"pipeline."+key);}
    private Map<String,Object> opportunity(CommercialFixtures.Organization a) {
        var body=new HashMap<String,Object>(Map.of("title","Prueba de embudo","companyId",a.companyId(),"contactId",a.contactId(),"venueId",a.venueId(),"eventTypeId",a.eventTypeId(),"eventStart","2030-01-01T12:00:00Z","eventEnd","2030-01-01T16:00:00Z","attendeeCount",20));
        body.put("salesRepId",a.seller().id());return body;
    }
    private Map<String,Object> stage(long pipeline,String name,String kind,boolean active){return Map.of("pipelineId",pipeline,"name",name,"kind",kind,"active",active);}
    private Map<String,Object> entry(String name,boolean active){return Map.of("name",name,"active",active);}
    private long create(String name,String template,HttpHeaders h){var body=new HashMap<String,Object>();body.put("name",name);if(template!=null)body.put("templateKey",template);return number(send("pipelines",HttpMethod.POST,body,h,201),"id");}
    private List<Map<String,Object>> stages(long pipeline,HttpHeaders h){return array("stages?pipelineId="+pipeline,h);}
    private List<Long> ids(long pipeline,HttpHeaders h){return stages(pipeline,h).stream().map(v->number(v,"id")).toList();}
    private void reorder(long pipeline,List<Long> ids,HttpHeaders h,int status){var response=http.exchange("/api/v1/pipelines/"+pipeline+"/stages/reorder",HttpMethod.POST,new HttpEntity<>(Map.of("stageIds",ids),h),String.class);assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(status);}
    private List<Map<String,Object>> array(String path,HttpHeaders h){var r=http.exchange("/api/v1/"+path,HttpMethod.GET,new HttpEntity<>(h),new ParameterizedTypeReference<List<Map<String,Object>>>(){});assertThat(r.getStatusCode().value()).isEqualTo(200);return r.getBody();}
    private HttpHeaders login(CommercialFixtures.Account account){var r=http.postForEntity("/api/v1/auth/login",new LoginRequest(account.email(),CommercialFixtures.PASSWORD),LoginResponse.class);assertThat(r.getStatusCode().value()).isEqualTo(200);var h=new HttpHeaders();h.setBearerAuth(r.getBody().accessToken());return h;}
    private Map<String,Object> send(String path,HttpMethod method,Object body,HttpHeaders h,int status){var r=http.exchange("/api/v1/"+path,method,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});assertThat(r.getStatusCode().value()).as(method+" "+path+": "+r.getBody()).isEqualTo(status);return r.getBody();}
    private long number(Map<String,Object> value,String key){return ((Number)value.get(key)).longValue();}
}
