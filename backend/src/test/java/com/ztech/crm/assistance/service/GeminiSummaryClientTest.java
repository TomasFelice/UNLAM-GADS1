package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

class GeminiSummaryClientTest {
    final JsonMapper json=JsonMapper.builder().build();
    final SummarySettings settings=new SummarySettings(new MockEnvironment().withProperty("GEMINI_API_KEY","fictitious-secret"));
    @Test void sendsFixedProtocolAndBoundedBodyWithTimeoutAndNoSecretInUrl() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var mode=new AtomicReference<>("valid");var received=new AtomicReference<String>();
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/models/",exchange->{
            received.set(exchange.getRequestURI()+" "+new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            assertThat(exchange.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo("fictitious-secret");
            try {
                if(mode.get().equals("slow"))Thread.sleep(300);
                if(mode.get().equals("slowBody")) {
                    exchange.sendResponseHeaders(200,200);exchange.getResponseBody().write(123);exchange.getResponseBody().flush();Thread.sleep(300);return;
                }
                byte[] body=(mode.get().equals("large")?"x".repeat(65537):"{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}],\"usageMetadata\":{\"promptTokenCount\":12}}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);
            }catch(Exception ignored){}finally{exchange.close();}
        });server.start();
        try {
            var client=new GeminiSummaryClient(settings,json,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/models/"));
            assertThat(client.generate("gemini-3.1-flash-lite","fictitious context",Duration.ofSeconds(2)).inputTokens()).isEqualTo(12);
            assertThat(received.get()).startsWith("/models/gemini-3.1-flash-lite:generateContent ").contains("responseJsonSchema","maxOutputTokens").doesNotContain("fictitious-secret","\"tools\"");
            mode.set("large");assertThatThrownBy(()->client.generate("gemini-3.1-flash-lite","x",Duration.ofSeconds(2)))
                    .isInstanceOfSatisfying(ProviderFailure.class,e->assertThat(e.kind()).isEqualTo(ProviderFailure.Kind.INVALID));
            mode.set("slow");assertThatThrownBy(()->client.generate("gemini-3.1-flash-lite","x",Duration.ofMillis(50)))
                    .isInstanceOf(ProviderFailure.class);
            mode.set("slowBody");assertThatThrownBy(()->client.generate("gemini-3.1-flash-lite","x",Duration.ofMillis(100)))
                    .isInstanceOfSatisfying(ProviderFailure.class,e->assertThat(e.kind()).isEqualTo(ProviderFailure.Kind.TIMEOUT));
        }finally{server.stop(0);}
    }
    @Test void unknownOrDailyQuotaNeverBecomesTransientEvenWithRetryAfter() {
        var client=new GeminiSummaryClient(settings,json);
        var headers=HttpHeaders.of(Map.of("Retry-After",List.of("2")),(a,b)->true);
        assertThat(client.classify(429,headers,"{}").kind()).isEqualTo(ProviderFailure.Kind.QUOTA);
        String quota="{\"error\":{\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.QuotaFailure\",\"violations\":[{\"quotaId\":\"RequestsPerMinute\"}]}]}}";
        assertThat(client.classify(429,headers,quota).kind()).isEqualTo(ProviderFailure.Kind.TEMPORARY_RATE);
        assertThat(client.classify(429,headers,quota.replace("PerMinute","PerDay")).kind()).isEqualTo(ProviderFailure.Kind.QUOTA);
        for(int status:new int[]{400,401,403,404})assertThat(client.classify(status,headers,"fake secret").kind()).isEqualTo(ProviderFailure.Kind.UNAVAILABLE);
        assertThat(client.classify(503,headers,"secret").kind()).isEqualTo(ProviderFailure.Kind.TRANSIENT);
    }
}
