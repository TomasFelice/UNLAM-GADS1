package com.ztech.crm.assistance.service;

import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** La configuración inválida deshabilita únicamente IA, sin impedir el arranque. */
@Component
public class SummarySettings {
    private static final Set<String> MODELS = Set.of("gemini-3.1-flash-lite", "gemini-3.5-flash-lite");
    private final Environment environment;
    public SummarySettings(Environment environment) { this.environment = environment; }
    private String value(String key, String fallback) { return environment.getProperty(key, fallback).strip(); }
    public String apiKey() { return value("GEMINI_API_KEY", ""); }
    public String model() { return value("GEMINI_SUMMARY_MODEL", "gemini-3.1-flash-lite"); }
    public String fallbackModel() { return value("GEMINI_SUMMARY_FALLBACK_MODEL", ""); }
    public int timeoutSeconds() { return bounded("AI_SUMMARY_TIMEOUT_SECONDS", 40, 6, 40); }
    public int maxAttempts() { return bounded("AI_SUMMARY_MAX_ATTEMPTS", 3, 1, 3); }
    public int callsPerMinute() { return bounded("AI_SUMMARY_MAX_CALLS_PER_MINUTE", 3, 1, 3); }
    public int callsPerDay() { return bounded("AI_SUMMARY_MAX_CALLS_PER_DAY", 20, 1, 20); }
    private int bounded(String key, int initial, int min, int max) {
        try { int n = Integer.parseInt(value(key, String.valueOf(initial))); return n >= min && n <= max ? n : 0; }
        catch (NumberFormatException ex) { return 0; }
    }
    public boolean configured() {
        return value("AI_SUMMARY_ENABLED", "false").equalsIgnoreCase("true") && apiKey().matches("[A-Za-z0-9._-]+")
                && MODELS.contains(model()) && (fallbackModel().isEmpty() || MODELS.contains(fallbackModel()))
                && timeoutSeconds() > 0 && maxAttempts() > 0 && callsPerMinute() > 0 && callsPerDay() > 0;
    }
}
