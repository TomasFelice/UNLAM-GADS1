package com.ztech.crm.opportunities;

import static org.assertj.core.api.Assertions.assertThat;

import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class OpportunityCreationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("DB_URL", postgres::getJdbcUrl); r.add("DB_USERNAME", postgres::getUsername); r.add("DB_PASSWORD", postgres::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void derivesCompanyAndOwnerFromContactAndPreservesIndividualClients() {
        var a=CommercialFixtures.create(jdbc,encoder,"creation.context").first();var h=login(a.admin());
        var request=opportunity(a);request.remove("companyId");request.remove("salesRepId");
        var created=send("opportunities",HttpMethod.POST,request,h,201);
        assertThat(created).containsEntry("companyId",(int)a.companyId()).containsEntry("contactId",(int)a.contactId()).containsEntry("salesRepId",(int)a.otherSeller().id());
        request.remove("contactId");request.put("companyId",a.companyId());
        assertThat(send("opportunities",HttpMethod.POST,request,h,201)).containsEntry("salesRepId",(int)a.otherSeller().id());
        var individual=send("contacts",HttpMethod.POST,Map.of("firstName","Individual","lastName","Contextual","status","POTENCIAL","salesRepId",a.seller().id()),h,201);
        assertThat(send("contacts?q=Individual",HttpMethod.GET,null,h,200)).containsEntry("totalElements",1);
        request.remove("companyId");request.put("contactId",individual.get("id"));
        assertThat(send("opportunities",HttpMethod.POST,request,h,201)).containsEntry("salesRepId",(int)a.seller().id()).containsEntry("companyId",null);
        var seller=login(a.seller());
        assertThat(send("opportunities",HttpMethod.POST,request,seller,201)).containsEntry("salesRepId",(int)a.seller().id());
    }
    @Test void rejectsContradictoryAndForeignContextWithoutPartialOpportunityOrHistory() {
        var scenario=CommercialFixtures.create(jdbc,encoder,"creation.invalid");var a=scenario.first();var h=login(a.admin());
        var other=send("companies",HttpMethod.POST,Map.of("legalName","Otra empresa","businessName","Otra empresa","status","POTENCIAL"),h,201);
        int opportunities=jdbc.queryForObject("SELECT count(*) FROM opportunities WHERE tenant_id=?",Integer.class,a.id());
        int history=jdbc.queryForObject("SELECT count(*) FROM stage_history WHERE tenant_id=?",Integer.class,a.id());
        var request=opportunity(a);request.put("companyId",other.get("id"));
        assertThat(send("opportunities",HttpMethod.POST,request,h,422)).containsEntry("code","CONTACT_COMPANY_MISMATCH");
        for(var account:List.of(a.admin(),a.manager(),a.seller())) {
            var role=login(account);
            send("contacts/"+scenario.second().contactId(),HttpMethod.GET,null,role,404);
            send("companies/"+scenario.second().companyId(),HttpMethod.GET,null,role,404);
            request.remove("companyId");request.put("contactId",scenario.second().contactId());
            send("opportunities",HttpMethod.POST,request,role,404);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunities WHERE tenant_id=?",Integer.class,a.id())).isEqualTo(opportunities);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stage_history WHERE tenant_id=?",Integer.class,a.id())).isEqualTo(history);
    }
    @Test void requiresRealDatesAndAllowsSixtyWithinVenueCapacityWithExactRoundTrip() {
        var a=CommercialFixtures.create(jdbc,encoder,"creation.event").first();var h=login(a.admin());var request=opportunity(a);
        request.remove("eventStart");send("opportunities",HttpMethod.POST,request,h,400);
        request.put("eventStart","2031-02-01T09:00:00-03:00");request.put("eventEnd","2031-02-01T13:00:00-03:00");request.put("attendeeCount",81);
        assertThat(send("opportunities",HttpMethod.POST,request,h,422)).containsEntry("code","VENUE_CAPACITY_EXCEEDED");
        request.put("attendeeCount",60);long id=number(send("opportunities",HttpMethod.POST,request,h,201),"id");
        var detail=send("opportunities/"+id,HttpMethod.GET,null,h,200);
        assertThat(detail).containsEntry("attendeeCount",60).containsEntry("eventStart","2031-02-01T12:00:00Z").containsEntry("eventEnd","2031-02-01T16:00:00Z");
    }
    private Map<String,Object> opportunity(CommercialFixtures.Organization a) {
        var body=new HashMap<String,Object>(Map.of("title","Alta contextual","companyId",a.companyId(),"contactId",a.contactId(),"venueId",a.venueId(),"eventTypeId",a.eventTypeId(),"eventStart","2031-02-01T12:00:00Z","eventEnd","2031-02-01T16:00:00Z","attendeeCount",20));
        body.put("pipelineId",a.pipelineId());body.put("stageId",a.stageId());body.put("salesRepId",a.seller().id());return body;
    }
    private HttpHeaders login(CommercialFixtures.Account account){var r=http.postForEntity("/api/v1/auth/login",new LoginRequest(account.email(),CommercialFixtures.PASSWORD),LoginResponse.class);assertThat(r.getStatusCode().value()).isEqualTo(200);var h=new HttpHeaders();h.setBearerAuth(r.getBody().accessToken());return h;}
    private Map<String,Object> send(String path,HttpMethod method,Object body,HttpHeaders h,int status){var r=http.exchange("/api/v1/"+path,method,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});assertThat(r.getStatusCode().value()).as(method+" "+path+": "+r.getBody()).isEqualTo(status);return r.getBody();}
    private long number(Map<String,Object> value,String key){return ((Number)value.get(key)).longValue();}
}
