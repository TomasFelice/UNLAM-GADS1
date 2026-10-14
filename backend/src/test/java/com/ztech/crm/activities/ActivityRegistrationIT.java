package com.ztech.crm.activities;

import static org.assertj.core.api.Assertions.assertThat;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.support.CommercialFixtures;
import java.util.*;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
class ActivityRegistrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("DB_URL",postgres::getJdbcUrl);r.add("DB_USERNAME",postgres::getUsername);r.add("DB_PASSWORD",postgres::getPassword);
    }
    @Autowired TestRestTemplate http; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder;

    @Test void recordsRealFactsOnOwnOpenAndClosedOpportunityWithServerActorAndDerivedParties() {
        var f=fixture("context");var a=f.first();var seller=login(a.seller());var request=body(a);
        request.put("opportunityId",a.ownOpportunityId());request.put("createdBy",f.second().admin().id());request.put("tenantId",f.second().id());request.put("createdAt","2000-01-01T00:00:00Z");
        Instant before=Instant.now();var response=send("activities",HttpMethod.POST,request,seller,201);
        assertThat(response).containsEntry("companyId",(int)a.companyId()).containsEntry("contactId",(int)a.contactId());
        assertThat(((Map<?,?>)response.get("author")).get("id")).isEqualTo((int)a.seller().id());
        assertThat(Instant.parse((String)response.get("createdAt"))).isBetween(before,Instant.now());
        assertThat(response).containsEntry("occurredAt","2026-01-01T12:00:00Z");
        long reason=jdbc.queryForObject("SELECT id FROM loss_reasons WHERE tenant_id=?",Long.class,a.id());
        long stage=jdbc.queryForObject("SELECT id FROM stages WHERE tenant_id=? AND kind='LOST'",Long.class,a.id());
        send("opportunities/"+a.ownOpportunityId()+"/lose",HttpMethod.POST,Map.of("stageId",stage,"lossReasonId",reason,"version",0),seller,200);
        send("activities",HttpMethod.POST,request,seller,201);
        assertThat(send("opportunities/"+a.ownOpportunityId(),HttpMethod.GET,null,seller,200)).containsEntry("status","PERDIDA").containsEntry("version",1);
    }

    @Test void derivesCompanyFromDirectContactAndKeepsIndividualWithoutCompany() {
        var a=fixture("contact").first();var seller=login(a.seller());var admin=login(a.admin());
        jdbc.update("UPDATE contacts SET sales_rep_id=? WHERE id=?",a.seller().id(),a.contactId());
        var request=body(a);request.put("contactId",a.contactId());
        assertThat(send("activities",HttpMethod.POST,request,seller,201)).containsEntry("companyId",(int)a.companyId());
        long individual=((Number)send("contacts",HttpMethod.POST,Map.of("firstName","Individual","lastName","Actividad","status","POTENCIAL","salesRepId",a.seller().id()),admin,201).get("id")).longValue();
        request.put("contactId",individual);
        assertThat(send("activities",HttpMethod.POST,request,seller,201)).containsEntry("companyId",null);
        request.put("companyId",a.companyId());
        assertThat(send("activities",HttpMethod.POST,request,seller,422)).containsEntry("code","ACTIVITY_ASSOCIATION_MISMATCH");
        request.remove("contactId");jdbc.update("UPDATE companies SET sales_rep_id=?,status='INACTIVO' WHERE id=?",a.seller().id(),a.companyId());
        // Es un hecho ocurrido; la baja lógica conserva el cliente, no impide reconstruirlo.
        send("activities",HttpMethod.POST,request,seller,201);
    }

    @Test void rejectsFutureMissingContradictoryAndInactiveDataWithoutWriting() {
        var f=fixture("invalid");var a=f.first();var h=login(a.admin());var request=body(a);
        int before=count(a);
        assertThat(send("activities",HttpMethod.POST,request,h,422)).containsEntry("code","ACTIVITY_WITHOUT_PARTY");
        request.put("opportunityId",a.ownOpportunityId());request.put("companyId",f.second().companyId());
        send("activities",HttpMethod.POST,request,h,422);request.remove("companyId");
        request.put("contactId",f.second().contactId());send("activities",HttpMethod.POST,request,h,422);request.remove("contactId");
        request.put("occurredAt",Instant.now().plusSeconds(3600).toString());send("activities",HttpMethod.POST,request,h,400);
        request.put("occurredAt","2026-01-01T12:00:00Z");
        for(String field:List.of("description","result")) { request.put(field," ");send("activities",HttpMethod.POST,request,h,400);request.put(field,"Contenido real"); }
        request.remove("typeId");send("activities",HttpMethod.POST,request,h,400);request.put("typeId",type(a));
        jdbc.update("UPDATE activity_types SET active=false WHERE id=?",type(a));send("activities",HttpMethod.POST,request,h,422);
        request.put("typeId",type(f.second()));send("activities",HttpMethod.POST,request,h,404);
        assertThat(count(a)).isEqualTo(before);
    }

    @Test void indirectCustomerReadingDoesNotAuthorizeGeneralWritesAndForeignIdsRemainHidden() {
        var f=fixture("scope");var a=f.first();var seller=login(a.seller());var request=body(a);
        send("companies/"+a.companyId(),HttpMethod.GET,null,seller,200);
        request.put("companyId",a.companyId());send("activities",HttpMethod.POST,request,seller,404);
        request.remove("companyId");request.put("contactId",a.contactId());send("activities",HttpMethod.POST,request,seller,404);
        request.remove("contactId");request.put("opportunityId",a.otherOpportunityId());send("activities",HttpMethod.POST,request,seller,404);
        request.put("opportunityId",a.ownOpportunityId());send("activities",HttpMethod.POST,request,seller,201);
        for(var account:List.of(a.admin(),a.manager(),a.seller())) {
            var h=login(account);
            for(String field:List.of("companyId","contactId","opportunityId")) {
                var foreign=body(a);foreign.put(field,field.equals("companyId")?f.second().companyId():field.equals("contactId")?f.second().contactId():f.second().ownOpportunityId());
                send("activities",HttpMethod.POST,foreign,h,404);
            }
        }
    }

    @Test void sharedCustomerNeverExposesActivitiesOfAnotherSellersOpportunity() {
        var f=fixture("visibility");var a=f.first();var admin=login(a.admin());var general=body(a);general.put("companyId",a.companyId());
        long generalId=((Number)send("activities",HttpMethod.POST,general,admin,201).get("id")).longValue();
        long own=jdbc.queryForObject("SELECT id FROM activities WHERE opportunity_id=?",Long.class,a.ownOpportunityId());
        long other=jdbc.queryForObject("SELECT id FROM activities WHERE opportunity_id=?",Long.class,a.otherOpportunityId());
        for(var account:List.of(a.admin(),a.manager(),a.seller())) {
            var h=login(account);boolean seller=account.equals(a.seller());
            for(String path:List.of("activities","companies/"+a.companyId()+"/activities","contacts/"+a.contactId()+"/activities")) {
                var page=send(path,HttpMethod.GET,null,h,200);var ids=content(page).stream().map(v->((Number)v.get("id")).longValue()).toList();
                assertThat(ids).contains(own);if(seller)assertThat(ids).doesNotContain(other);else assertThat(ids).contains(other);
                if(!path.startsWith("contacts"))assertThat(ids).contains(generalId);
                assertThat(ids).doesNotContain(jdbc.queryForObject("SELECT min(id) FROM activities WHERE tenant_id=?",Long.class,f.second().id()));
            }
            assertThat(content(send("opportunities/"+a.ownOpportunityId()+"/activities",HttpMethod.GET,null,h,200))).hasSize(1);
            send("opportunities/"+a.otherOpportunityId()+"/activities",HttpMethod.GET,null,h,seller?404:200);
            send("companies/"+f.second().companyId()+"/activities",HttpMethod.GET,null,h,404);
            send("contacts/"+f.second().contactId()+"/activities",HttpMethod.GET,null,h,404);
            send("opportunities/"+f.second().ownOpportunityId()+"/activities",HttpMethod.GET,null,h,404);
        }
    }

    @Test void paginationTiesInactiveAuthorsAndLogicalCustomerDeactivationKeepAllFacts() {
        var a=fixture("pages").first();var h=login(a.admin());var author=login(a.otherSeller());var request=body(a);request.put("contactId",a.contactId());
        List<Long> expected=new ArrayList<>();
        for(int i=0;i<27;i++) { request.put("description","Hecho "+i);expected.add(((Number)send("activities",HttpMethod.POST,request,author,201).get("id")).longValue()); }
        Collections.reverse(expected);
        jdbc.update("UPDATE users SET active=false WHERE id=?",a.otherSeller().id());
        jdbc.update("UPDATE companies SET status='INACTIVO' WHERE id=?",a.companyId());jdbc.update("UPDATE contacts SET status='NO_CONTACTAR' WHERE id=?",a.contactId());
        var seen=new ArrayList<Long>();
        for(int page=0;page<2;page++) {
            var result=send("contacts/"+a.contactId()+"/activities?page="+page,HttpMethod.GET,null,h,200);
            assertThat(result).containsEntry("totalElements",29);
            seen.addAll(content(result).stream().map(v->((Number)v.get("id")).longValue()).toList());
            assertThat(content(result).toString()).contains("other");
        }
        assertThat(seen).hasSize(29).doesNotHaveDuplicates();
        // Dos hechos previos de octubre aparecen antes que los 27 de enero.
        assertThat(seen.subList(2,seen.size())).containsExactlyElementsOf(expected);
        assertThat(send("activities/"+expected.getFirst(),HttpMethod.PUT,request,h,404)).containsEntry("status",404);
        send("activities/"+expected.getFirst(),HttpMethod.DELETE,null,h,404);
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> content(Map<String,Object> page){return (List<Map<String,Object>>)page.get("content");}
    @Test void customerRelationsKeepStablePagesWithTiedDatesAndLogicalDeactivation() {
        var a=fixture("relations").first();var h=login(a.admin());
        for(int i=0;i<25;i++) {
            jdbc.update("""
                INSERT INTO opportunities (pipeline_id,tenant_id,title,company_id,contact_id,sales_rep_id,
                    venue_id,stage_id,event_type_id,status,event_start,event_end,attendee_count)
                SELECT pipeline_id,tenant_id,title,company_id,contact_id,sales_rep_id,
                    venue_id,stage_id,event_type_id,status,event_start,event_end,attendee_count
                FROM opportunities WHERE id=?
                """,a.ownOpportunityId());
            jdbc.update("""
                INSERT INTO contacts (tenant_id,company_id,first_name,last_name,status,sales_rep_id)
                SELECT tenant_id,company_id,first_name,last_name,status,sales_rep_id FROM contacts WHERE id=?
                """,a.contactId());
        }
        jdbc.update("UPDATE companies SET status='INACTIVO' WHERE id=?",a.companyId());
        jdbc.update("UPDATE contacts SET status='INACTIVO' WHERE tenant_id=?",a.id());
        for(String path:List.of("opportunities?companyId="+a.companyId()+"&sort=eventStart,desc",
                "opportunities?contactId="+a.contactId()+"&sort=eventStart,desc",
                "contacts?companyId="+a.companyId()+"&sort=lastName,asc")) {
            List<Long> ids=new ArrayList<>();
            int expected=path.startsWith("contacts")?26:27;
            for(int page=0;page<2;page++) {
                var result=send(path+"&page="+page,HttpMethod.GET,null,h,200);
                assertThat(result).containsEntry("totalElements",expected);
                ids.addAll(content(result).stream().map(v->((Number)v.get("id")).longValue()).toList());
            }
            assertThat(ids).hasSize(expected).doesNotHaveDuplicates().isSorted();
            assertThat(content(send(path,HttpMethod.GET,null,h,200))).hasSize(20);
        }
    }
    private int count(CommercialFixtures.Organization a){return jdbc.queryForObject("SELECT count(*) FROM activities WHERE tenant_id=?",Integer.class,a.id());}
    private long type(CommercialFixtures.Organization a){return jdbc.queryForObject("SELECT id FROM activity_types WHERE tenant_id=? ORDER BY id LIMIT 1",Long.class,a.id());}
    private Map<String,Object> body(CommercialFixtures.Organization a){return new HashMap<>(Map.of("typeId",type(a),"occurredAt","2026-01-01T12:00:00Z","description","Hecho ocurrido","result","Interés confirmado"));}
    private CommercialFixtures.Scenario fixture(String key){return CommercialFixtures.create(jdbc,encoder,"activity."+key);}
    private HttpHeaders login(CommercialFixtures.Account account){var r=http.postForEntity("/api/v1/auth/login",new LoginRequest(account.email(),CommercialFixtures.PASSWORD),LoginResponse.class);assertThat(r.getStatusCode().value()).isEqualTo(200);var h=new HttpHeaders();h.setBearerAuth(r.getBody().accessToken());return h;}
    private Map<String,Object> send(String path,HttpMethod method,Object body,HttpHeaders h,int status){var r=http.exchange("/api/v1/"+path,method,new HttpEntity<>(body,h),new ParameterizedTypeReference<Map<String,Object>>(){});assertThat(r.getStatusCode().value()).as(path+": "+r.getBody()).isEqualTo(status);return r.getBody();}
}
