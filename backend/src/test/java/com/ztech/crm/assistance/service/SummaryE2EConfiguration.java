package com.ztech.crm.assistance.service;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import tools.jackson.databind.json.JsonMapper;

/** Sólo classpath de pruebas. Nunca se incluye en el JAR de producción. */
@TestConfiguration(proxyBeanMethods=false)
public class SummaryE2EConfiguration {
    private final AtomicReference<String> mode=new AtomicReference<>("success");
    private final AtomicInteger calls=new AtomicInteger();
    @Bean @Primary SummarySettings summaryE2ESettings() {
        return new SummarySettings(new MockEnvironment().withProperty("AI_SUMMARY_ENABLED","true")
                .withProperty("GEMINI_API_KEY","e2e-fictitious-key")) {
            @Override public boolean configured(){return !mode.get().equals("disabled");}
            @Override public int maxAttempts(){return 1;}
        };
    }
    @Bean @Primary SummaryAdmission summaryE2EAdmission(SummarySettings settings,SummaryTime time) {
        return new SummaryAdmission(settings,time){@Override public void countAttempt(){}};
    }
    @Bean(destroyMethod="close") Stub summaryE2EStub(JsonMapper json) throws Exception {return new Stub(json);}
    @Bean @Primary SummaryGenerator summaryE2EGenerator(SummarySettings settings,JsonMapper json,Stub stub) {
        return new GeminiSummaryClient(settings,json,URI.create("http://127.0.0.1:"+stub.server.getAddress().getPort()+"/models/"));
    }
    @Bean FilterRegistrationBean<OncePerRequestFilter> summaryE2EControl(JsonMapper json) {
        var registration=new FilterRegistrationBean<OncePerRequestFilter>();
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setFilter(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws java.io.IOException,jakarta.servlet.ServletException {
                if(!req.getRequestURI().equals("/__tests/summary")){chain.doFilter(req,res);return;}
                if(req.getMethod().equals("POST")) {
                    var value=req.getParameter("mode");
                    if(value!=null&&Set.of("success","quota","timeout","unavailable","disabled").contains(value)){mode.set(value);calls.set(0);}
                }
                res.setContentType("application/json");res.getWriter().write(json.writeValueAsString(Map.of("calls",calls.get(),"mode",mode.get())));
            }
        });return registration;
    }
    final class Stub implements AutoCloseable {
        final HttpServer server;
        Stub(JsonMapper json) throws Exception {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/models/",exchange->{
                calls.incrementAndGet();String selected=mode.get();
                try {
                    var request=json.readTree(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                    var context=json.readTree(request.path("contents").get(0).path("parts").get(0).path("text").asString());
                    var sourceIds=new ArrayList<String>();sourceIds.add("O");
                    for(var source:context.path("sources"))if(source.path("type").asString().equals("ACTIVITY")){sourceIds.add(source.path("sourceId").asString());break;}
                    if(selected.equals("timeout"))Thread.sleep(11000);
                    int status=selected.equals("quota")?429:selected.equals("unavailable")?401:200;
                    var sections=Map.of("clientNeeds",List.of(),"currentSituation",List.of(Map.of("text","Síntesis ficticia de los hechos registrados", "sourceIds",sourceIds)),"agreementsAndObjections",List.of(),"unconfirmedInformation",List.of());
                    String body=status==200?json.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",json.writeValueAsString(sections)))))))):"{}";
                    byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
                }catch(Exception ignored){}finally{exchange.close();}
            });server.start();
        }
        @Override public void close(){server.stop(0);}
    }
}
