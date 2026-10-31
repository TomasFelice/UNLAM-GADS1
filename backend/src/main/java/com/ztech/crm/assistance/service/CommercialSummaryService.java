package com.ztech.crm.assistance.service;

import com.ztech.crm.assistance.dto.CommercialSummaryResponse;
import java.time.Duration;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class CommercialSummaryService {
    private final SummarySnapshotReader snapshots;
    private final SummarySettings settings;
    private final SummaryGenerator generator;
    private final SummaryResultValidator validator;
    private final SummaryAdmission admission;
    private final SummaryTime time;
    public CommercialSummaryService(SummarySnapshotReader snapshots,SummarySettings settings,SummaryGenerator generator,
            SummaryResultValidator validator,SummaryAdmission admission,SummaryTime time) {
        this.snapshots=snapshots;this.settings=settings;this.generator=generator;this.validator=validator;this.admission=admission;this.time=time;
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'SELLER')")
    public CommercialSummaryResponse generate(Long id) {
        long start=time.nanos();long deadline=start+Duration.ofSeconds(settings.timeoutSeconds()).toNanos();
        var snapshot=snapshots.read(id); // Autorizar antes de configuración, admisión o proveedor.
        if(!settings.configured()) {
            LoggerFactory.getLogger(CommercialSummaryService.class).warn("AI summary configuration unavailable; code=AI_NOT_CONFIGURED");
            throw new SummaryFailure(HttpStatus.SERVICE_UNAVAILABLE,"AI_NOT_CONFIGURED","El resumen con IA no está habilitado.");
        }
        int attempts=0;String model=settings.model();String outcome="SUCCESS";
        Long inputTokens=null,outputTokens=null;
        try(var permit=admission.acquire()) {
            while(true) {
                long remaining=(deadline-time.nanos())/1_000_000-5000;
                if(remaining<=0)throw SummaryFailure.timeout();
                admission.countAttempt();attempts++;
                try {
                    var generated=generator.generate(model,snapshot.context(),Duration.ofMillis(Math.min(10_000,remaining)));
                    inputTokens=generated.inputTokens();outputTokens=generated.outputTokens();
                    var sections=validator.validate(generated.text(),snapshot.sources().stream().map(s->s.sourceId()).collect(Collectors.toSet()));
                    if(time.nanos()>=deadline)throw SummaryFailure.timeout();
                    var current=snapshots.read(id); // Otra transacción breve; nunca durante la red.
                    if(time.nanos()>=deadline)throw SummaryFailure.timeout();
                    if(!snapshot.fingerprint().equals(current.fingerprint()))throw new SummaryFailure(HttpStatus.CONFLICT,"AI_CONTEXT_CHANGED","La oportunidad cambió. Reintentá el resumen.");
                    return new CommercialSummaryResponse(id,time.now(),snapshot.contextAsOf(),snapshot.version(),
                            snapshot.coverage(),sections,snapshot.sources());
                }catch(ProviderFailure failure) {
                    boolean retry=switch(failure.kind()){case TRANSIENT,TEMPORARY_RATE,TIMEOUT->true;default->false;};
                    if(!retry||attempts>=settings.maxAttempts())throw translate(failure);
                    long backoff=Math.min(4000,1000L<<(attempts-1))+time.jitterMillis();
                    long delay=failure.retryAfterSeconds()==null?backoff:Math.max(backoff,safeMillis(failure.retryAfterSeconds()));
                    long available=(deadline-time.nanos())/1_000_000-5000;
                    if(delay>=available)throw failure.kind()==ProviderFailure.Kind.TEMPORARY_RATE?translate(failure):SummaryFailure.timeout();
                    time.sleep(delay);
                    if(attempts==2 && (failure.kind()==ProviderFailure.Kind.TRANSIENT||failure.kind()==ProviderFailure.Kind.TIMEOUT)
                            && !settings.fallbackModel().isEmpty())model=settings.fallbackModel();
                }
            }
        }catch(com.ztech.crm.shared.exception.ApiException ex){outcome=ex.getCode();throw ex;}
        finally {
            LoggerFactory.getLogger(CommercialSummaryService.class).info(
                    "AI summary model={} promptVersion={} durationMs={} attempts={} outcome={} inputTokens={} outputTokens={}",
                    model,SummaryPrompt.VERSION,(time.nanos()-start)/1_000_000,attempts,outcome,inputTokens,outputTokens);
        }
    }
    private long safeMillis(long seconds){return seconds>Long.MAX_VALUE/1000?Long.MAX_VALUE:Math.max(0,seconds)*1000;}
    private SummaryFailure translate(ProviderFailure failure) {
        return switch(failure.kind()) {
            case INVALID -> SummaryFailure.invalid();
            case BLOCKED -> new SummaryFailure(HttpStatus.UNPROCESSABLE_ENTITY,"AI_CONTENT_BLOCKED","No se pudo resumir este contenido.");
            case QUOTA,TEMPORARY_RATE -> new SummaryFailure(HttpStatus.TOO_MANY_REQUESTS,"AI_QUOTA_EXCEEDED","La cuota del proveedor no permite generar el resumen. Intentá más tarde.",failure.retryAfterSeconds());
            case TIMEOUT -> SummaryFailure.timeout();
            default -> SummaryFailure.unavailable();
        };
    }
}
