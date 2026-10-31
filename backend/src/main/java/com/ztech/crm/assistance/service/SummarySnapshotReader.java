package com.ztech.crm.assistance.service;

import com.ztech.crm.activities.service.ActivityService;
import com.ztech.crm.opportunities.service.OpportunitySummaryReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummarySnapshotReader {
    private final OpportunitySummaryReader opportunities;
    private final ActivityService activities;
    private final SummarySnapshotBuilder builder;
    public SummarySnapshotReader(OpportunitySummaryReader opportunities, ActivityService activities,
            SummarySnapshotBuilder builder) { this.opportunities=opportunities; this.activities=activities; this.builder=builder; }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, timeout=5)
    public SummarySnapshot read(Long id) {
        var opportunity=opportunities.read(id);
        return builder.build(opportunity,activities.summaryContext(id));
    }
}
