package com.ztech.crm.assistance.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SummaryTime {
    private final Clock clock;
    public SummaryTime(){this(Clock.systemUTC());}
    SummaryTime(Clock clock){this.clock=clock;}
    public Instant now(){return clock.instant();}
    public long nanos(){return System.nanoTime();}
    public void sleep(long millis) {
        try {Thread.sleep(millis);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw SummaryFailure.timeout();}
    }
    public long jitterMillis(){return java.util.concurrent.ThreadLocalRandom.current().nextLong(251);}
}
