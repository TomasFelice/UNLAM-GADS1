package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SummaryAdmissionTest {
    static class Time extends SummaryTime {Instant instant=Instant.parse("2026-10-31T23:59:00Z");@Override public Instant now(){return instant;}}
    @Test void concurrentRequestsAdmitExactlyOneAndReleaseOnFailure() throws Exception {
        var admission=new SummaryAdmission(new SummarySettings(new MockEnvironment()),new Time());
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var first=Thread.ofVirtual().start(()->{try(var p=admission.acquire()){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}});
        entered.await();assertThatThrownBy(admission::acquire).isInstanceOf(SummaryFailure.class);
        release.countDown();first.join();try(var p=admission.acquire()){assertThat(p).isNotNull();}
    }
    @Test void serializesAndCountsEveryAttemptWithMinuteAndUtcDayWindows() {
        var time=new Time();var env=new MockEnvironment().withProperty("AI_SUMMARY_MAX_CALLS_PER_DAY","4");
        var admission=new SummaryAdmission(new SummarySettings(env),time);
        try(var permit=admission.acquire()) {
            assertThatThrownBy(admission::acquire).isInstanceOf(SummaryFailure.class);
            for(int i=0;i<3;i++)admission.countAttempt();
            assertThatThrownBy(admission::countAttempt).isInstanceOf(SummaryFailure.class);
        }
        time.instant=time.instant.plusSeconds(60);
        try(var permit=admission.acquire()){for(int i=0;i<3;i++)admission.countAttempt();}
        time.instant=time.instant.plusSeconds(60);admission.countAttempt();
        assertThatThrownBy(admission::countAttempt).isInstanceOf(SummaryFailure.class);
        time.instant=time.instant.plusSeconds(86400);admission.countAttempt();
        try(var permit=admission.acquire()){assertThat(permit).isNotNull();}
    }
}
