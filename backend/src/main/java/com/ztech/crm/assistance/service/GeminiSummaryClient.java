package com.ztech.crm.assistance.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.ztech.crm.assistance.service.ProviderFailure.Kind.*;

@Component
public class GeminiSummaryClient implements SummaryGenerator {
    private final HttpClient http;
    private final SummarySettings settings;
    private final JsonMapper json;
    private final URI endpoint;
    @Autowired
    public GeminiSummaryClient(SummarySettings settings,JsonMapper json) {
        this(settings,json,URI.create("https://generativelanguage.googleapis.com/v1beta/models/"));
    }
    // Endpoint inyectable sólo por construcción en tests; no hay propiedad configurable ni entrada HTTP.
    GeminiSummaryClient(SummarySettings settings,JsonMapper json,URI endpoint) {
        this.settings=settings;this.json=json;this.endpoint=endpoint;
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @Override public Generated generate(String model,String context,Duration timeout) {
        var body=Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",SummaryPrompt.SYSTEM))),
                "contents",List.of(Map.of("role","user","parts",List.of(Map.of("text",context)))),
                "generationConfig",Map.of("responseMimeType","application/json","responseJsonSchema",SummaryPrompt.schema(),
                        "maxOutputTokens",2048));
        var request=HttpRequest.newBuilder(URI.create(endpoint.toString()+model+":generateContent"))
                .timeout(timeout).header("Content-Type","application/json").header("x-goog-api-key",settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var subscriber=new LimitedBody();
        var pending=http.sendAsync(request,info->subscriber);
        try {
            var response=pending.get(timeout.toMillis(),TimeUnit.MILLISECONDS);
            String text=new String(response.body(),StandardCharsets.UTF_8);
            if(response.statusCode()!=200) throw classify(response.statusCode(),response.headers(),text);
            return decode(text);
        } catch(TimeoutException ex) {throw new ProviderFailure(TIMEOUT,null);}
        catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new ProviderFailure(TIMEOUT,null);}
        catch(ExecutionException ex) {
            if(ex.getCause() instanceof ProviderFailure p)throw p;
            if(ex.getCause() instanceof HttpTimeoutException)throw new ProviderFailure(TIMEOUT,null);
            throw new ProviderFailure(TRANSIENT,null);
        } finally {subscriber.cancel();pending.cancel(true);}
    }
    private Generated decode(String body) {
        try {
            var root=json.readTree(body);
            if(root.path("promptFeedback").has("blockReason"))throw new ProviderFailure(BLOCKED,null);
            var candidates=root.path("candidates");
            if(!candidates.isArray()||candidates.size()!=1)throw new ProviderFailure(INVALID,null);
            var c=candidates.get(0);String finish=c.path("finishReason").asString("");
            if(Set.of("SAFETY","RECITATION","BLOCKLIST","PROHIBITED_CONTENT","SPII").contains(finish))throw new ProviderFailure(BLOCKED,null);
            if(!finish.equals("STOP"))throw new ProviderFailure(INVALID,null);
            var parts=c.path("content").path("parts");
            if(!parts.isArray()||parts.size()!=1||!parts.get(0).path("text").isString())throw new ProviderFailure(INVALID,null);
            String text=parts.get(0).get("text").asString();if(text.isBlank())throw new ProviderFailure(INVALID,null);
            var usage=root.path("usageMetadata");
            return new Generated(text,token(usage,"promptTokenCount"),token(usage,"candidatesTokenCount"));
        }catch(ProviderFailure ex){throw ex;}catch(RuntimeException ex){throw new ProviderFailure(INVALID,null);}
    }
    private Long token(JsonNode node,String key){return node.path(key).isIntegralNumber()?node.get(key).asLong():null;}
    ProviderFailure classify(int status,HttpHeaders headers,String body) {
        if(status==429) {
            Long delay=retryAfter(headers);
            boolean daily=false, temporal=false;
            try {
                for(var detail:json.readTree(body).path("error").path("details")) {
                    if(detail.path("@type").asString("").endsWith("QuotaFailure")) {
                        for(var violation:detail.path("violations")) {
                            var quota=violation.path("quotaId").asString("").toLowerCase(Locale.ROOT);
                            if(quota.contains("perday")||violation.path("quotaValue").asString("").equals("0"))daily=true;
                            if(quota.contains("perminute"))temporal=true;
                        }
                    }
                    if(detail.path("@type").asString("").endsWith("RetryInfo")) {
                        var raw=detail.path("retryDelay").asString("");
                        if(raw.matches("[0-9]+(?:\\.[0-9]+)?s"))delay=(long)Math.ceil(Double.parseDouble(raw.substring(0,raw.length()-1)));
                    }
                }
            }catch(RuntimeException ignored){temporal=false;}
            return new ProviderFailure(!daily&&temporal&&delay!=null?TEMPORARY_RATE:QUOTA,delay);
        }
        return new ProviderFailure(Set.of(500,503,504).contains(status)?TRANSIENT:UNAVAILABLE,null);
    }
    private Long retryAfter(HttpHeaders headers) {
        String value=headers.firstValue("Retry-After").orElse("");
        try {return Math.max(0,Long.parseLong(value));}catch(NumberFormatException ignored) { }
        try {return Math.max(0,Duration.between(Instant.now(),ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toSeconds());}
        catch(RuntimeException ignored){return null;}
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private volatile Flow.Subscription subscription;
        @Override public CompletionStage<byte[]> getBody(){return result;}
        @Override public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
        @Override public void onNext(List<ByteBuffer> buffers) {
            for(var buffer:buffers) {
                if(buffer.remaining()>65536-bytes.size()) {cancel();result.completeExceptionally(new ProviderFailure(INVALID,null));return;}
                byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onComplete(){result.complete(bytes.toByteArray());}
        @Override public void onError(Throwable error){result.completeExceptionally(new ProviderFailure(TRANSIENT,null));}
        void cancel(){var s=subscription;if(s!=null)s.cancel();}
    }
}
