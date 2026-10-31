package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ztech.crm.assistance.dto.CommercialSummaryResponse.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

class CommercialSummaryServiceTest {
    static class Time extends SummaryTime {
        long n;
        @Override public long nanos(){return n;}
        @Override public void sleep(long millis){n+=millis*1_000_000;}
        @Override public long jitterMillis(){return 0;}
    }
    private final Time time=new Time();
    private final MockEnvironment env=new MockEnvironment().withProperty("AI_SUMMARY_ENABLED","true")
            .withProperty("GEMINI_API_KEY","fake").withProperty("GEMINI_SUMMARY_FALLBACK_MODEL","gemini-3.5-flash-lite");
    private final SummarySettings settings=new SummarySettings(env);
    private final SummarySnapshotReader reader=mock(SummarySnapshotReader.class);
    private final SummaryStore store=mock(SummaryStore.class);
    private final List<String> models=new ArrayList<>();
    private static final String EMPTY="{\"clientNeeds\":[],\"currentSituation\":[],\"agreementsAndObjections\":[],\"unconfirmedInformation\":[]}";
    private SummarySnapshot snapshot(String fingerprint){return new SummarySnapshot(1L,0L,Instant.EPOCH,fingerprint,"{}",null,List.of(new Source("O","OPPORTUNITY",1L,"Actual",null,null,Map.of(),false)));}
    private CommercialSummaryService service(SummaryGenerator generator) {
        when(reader.read(1L)).thenReturn(snapshot("same"));
        return new CommercialSummaryService(reader,settings,(model,context,timeout)->{models.add(model);return generator.generate(model,context,timeout);},
                new SummaryResultValidator(JsonMapper.builder().build()),new SummaryAdmission(settings,time),time,store);
    }
    @Test void retriesOnlyThreeCallsAndFallbackOnlyOnLastTransientAttempt() {
        var count=new AtomicInteger();var service=service((m,c,t)->{
            if(count.incrementAndGet()<3)throw new ProviderFailure(ProviderFailure.Kind.TRANSIENT,null);
            return new SummaryGenerator.Generated(EMPTY,1L,2L);
        });
        assertThat(service.generate(1L).sections().clientNeeds()).isEmpty();
        assertThat(models).containsExactly("gemini-3.1-flash-lite","gemini-3.1-flash-lite","gemini-3.5-flash-lite");
        assertThat(time.n).isEqualTo(3_000_000_000L);
    }
    @Test void quotaAndPermanentFailuresNeverRetryOrFallback() {
        for(var kind:List.of(ProviderFailure.Kind.QUOTA,ProviderFailure.Kind.UNAVAILABLE,ProviderFailure.Kind.INVALID,ProviderFailure.Kind.BLOCKED)) {
            models.clear();var service=service((m,c,t)->{throw new ProviderFailure(kind,null);});
            assertThatThrownBy(()->service.generate(1L)).isInstanceOf(SummaryFailure.class);assertThat(models).hasSize(1);
        }
    }
    @Test void knownMinuteDelayThatDoesNotFitNeverCallsAgain() {
        var service=service((m,c,t)->{throw new ProviderFailure(ProviderFailure.Kind.TEMPORARY_RATE,60L);});
        assertThatThrownBy(()->service.generate(1L)).isInstanceOfSatisfying(SummaryFailure.class,e->assertThat(e.getCode()).isEqualTo("AI_QUOTA_EXCEEDED"));
        assertThat(models).hasSize(1);assertThat(time.n).isZero();
    }
    @Test void temporaryQuotaRetriesPrimaryAndBudgetIncludesReadAndResponse() {
        var count=new AtomicInteger();var service=service((m,c,t)->{
            if(count.incrementAndGet()<3)throw new ProviderFailure(ProviderFailure.Kind.TEMPORARY_RATE,1L);
            time.n+=40_000_000_000L;return new SummaryGenerator.Generated(EMPTY,null,null);
        });
        assertThatThrownBy(()->service.generate(1L)).isInstanceOfSatisfying(SummaryFailure.class,e->assertThat(e.getCode()).isEqualTo("AI_TIMEOUT"));
        assertThat(models).containsOnly("gemini-3.1-flash-lite");verify(reader,times(1)).read(1L);
    }
    @Test void changedContextAndLostScopeDiscardResult() {
        var service=service((m,c,t)->new SummaryGenerator.Generated(EMPTY,null,null));
        when(reader.read(1L)).thenReturn(snapshot("before"),snapshot("after"));
        assertThatThrownBy(()->service.generate(1L)).isInstanceOfSatisfying(SummaryFailure.class,e->assertThat(e.getCode()).isEqualTo("AI_CONTEXT_CHANGED"));
        when(reader.read(1L)).thenReturn(snapshot("same")).thenThrow(new com.ztech.crm.shared.exception.NotFoundException("No encontrada"));
        assertThatThrownBy(()->service.generate(1L)).isInstanceOf(com.ztech.crm.shared.exception.NotFoundException.class);
    }
}
