package com.ztech.crm.assistance.service;

import java.time.Duration;
public interface SummaryGenerator {
    Generated generate(String model,String context,Duration timeout);
    record Generated(String text, Long inputTokens, Long outputTokens) { }
}
