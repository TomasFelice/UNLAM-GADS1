package com.ztech.crm.assistance.service;

import java.time.*;
import java.util.ArrayDeque;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

@Component
public class SummaryAdmission {
    private final SummarySettings settings;
    private final SummaryTime time;
    private final ArrayDeque<Instant> calls=new ArrayDeque<>();
    private LocalDate day;
    private int daily;
    private boolean busy;
    public SummaryAdmission(SummarySettings settings,SummaryTime time){this.settings=settings;this.time=time;}
    public synchronized Permit acquire() {
        if(busy)throw limited(1);
        busy=true;return new Permit();
    }
    public synchronized void countAttempt() {
        var now=time.now();var today=LocalDate.ofInstant(now,ZoneOffset.UTC);
        if(!today.equals(day)){day=today;daily=0;}
        while(!calls.isEmpty()&&!calls.getFirst().plusSeconds(60).isAfter(now))calls.removeFirst();
        if(daily>=settings.callsPerDay())throw limited(Math.max(1,Duration.between(now,today.plusDays(1).atStartOfDay(ZoneOffset.UTC)).toSeconds()));
        if(calls.size()>=settings.callsPerMinute())throw limited(Math.max(1,(Duration.between(now,calls.getFirst().plusSeconds(60)).toMillis()+999)/1000));
        calls.add(now);daily++;
    }
    private SummaryFailure limited(long seconds){return new SummaryFailure(HttpStatus.TOO_MANY_REQUESTS,"AI_RATE_LIMITED","El resumen alcanzó su límite temporal. Intentá más tarde.",seconds);}
    public final class Permit implements AutoCloseable {
        private boolean closed;
        @Override public void close(){synchronized(SummaryAdmission.this){if(!closed){closed=true;busy=false;}}}
    }
}
