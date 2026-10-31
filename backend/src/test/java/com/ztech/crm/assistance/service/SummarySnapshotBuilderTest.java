package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import com.ztech.crm.activities.service.ActivitySummaryContext;
import com.ztech.crm.opportunities.service.OpportunitySummaryContext;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SummarySnapshotBuilderTest {
    private final SummarySnapshotBuilder builder=new SummarySnapshotBuilder(JsonMapper.builder().findAndAddModules().build());
    private OpportunitySummaryContext opportunity(String notes,long count) {
        return new OpportunitySummaryContext(1L,2L,3L,"Capacitación ficticia",notes,"ABIERTA","Primera","OPEN","Corporativo",
                "Sala ficticia",30,null,List.of(),"Capacitación",Instant.EPOCH,Instant.EPOCH.plusSeconds(3600),20,
                null,null,null,null,null,null,count,List.of());
    }
    @Test void budgetPreservesUnicodeExactFragmentsCountsAndCanonicalFingerprint() {
        var facts=new ArrayList<ActivitySummaryContext.Fact>();
        for(long id=50;id>0;id--)facts.add(new ActivitySummaryContext.Fact(id,"Reunión",Instant.EPOCH.plusSeconds(id),
                Instant.EPOCH.plusSeconds(100),"😀".repeat(2500),"Acuerdo ficticio".repeat(200)));
        var o=opportunity("😀".repeat(4500),55);
        var a=new ActivitySummaryContext(80,facts);
        var snapshot=builder.build(o,a);
        assertThat(snapshot.context().codePointCount(0,snapshot.context().length())).isLessThanOrEqualTo(24000);
        assertThat(snapshot.coverage().partial()).isTrue();assertThat(snapshot.coverage().textTruncated()).isTrue();
        assertThat(snapshot.coverage().totalActivities()).isEqualTo(80);
        assertThat(snapshot.coverage().includedActivities()).isBetween(1,49);
        var current=snapshot.sources().stream().filter(s->s.sourceId().equals("O")).findFirst().orElseThrow();
        assertThat(current.fragments().get("notes")).isEqualTo("😀".repeat(4000));
        assertThat(current.fragments()).doesNotContainKeys("tenantId","email","phone","estimatedValueARS");
        assertThat(builder.build(o,a).fingerprint()).isEqualTo(snapshot.fingerprint());
        assertThat(builder.build(o,new ActivitySummaryContext(81,facts)).fingerprint()).isNotEqualTo(snapshot.fingerprint());
        assertThat(snapshot.sources().stream().filter(s->s.occurredAt()!=null).map(s->s.occurredAt())).isSorted();
    }
    @Test void absentDataIsNotZeroAndEmptyHistoryHasNoRange() {
        var snapshot=builder.build(opportunity(null,0),new ActivitySummaryContext(0,List.of()));
        assertThat(snapshot.coverage().partial()).isFalse();assertThat(snapshot.sources()).hasSize(1);
        assertThat(snapshot.coverage().activityRange().from()).isNull();
        assertThat(snapshot.sources().getFirst().fragments()).doesNotContainKeys("estimatedValueARS","optionalProbability","closedAt","notes");
    }
}
