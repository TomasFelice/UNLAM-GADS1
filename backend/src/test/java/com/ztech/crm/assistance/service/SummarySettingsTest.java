package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SummarySettingsTest {
    @Test void acceptsDottedAuthenticationKeyWithoutAllowingHeaderInjection() {
        var env=new MockEnvironment().withProperty("AI_SUMMARY_ENABLED","true")
                .withProperty("GEMINI_API_KEY","AQ.fictitious-test-key_not-a-real-credential");
        var settings=new SummarySettings(env);
        assertThat(settings.configured()).isTrue();
        for(String invalid:new String[]{"", "bad key", "AQ.fake\r\ninjected-header", "AQ.fake\nheader"}) {
            env.setProperty("GEMINI_API_KEY",invalid);assertThat(settings.configured()).isFalse();
        }
    }
    @Test void disabledWithoutSecretAndInvalidSettingsDoNotThrow() {
        var env=new MockEnvironment();var settings=new SummarySettings(env);
        assertThat(settings.configured()).isFalse();
        env.setProperty("AI_SUMMARY_ENABLED","true");assertThat(settings.configured()).isFalse();
        env.setProperty("GEMINI_API_KEY","fictitious-test-key");assertThat(settings.configured()).isTrue();
        for(var model:new String[]{"gemini-3.8-flash","gemini-flash-latest","gemini-3.1-flash-lite-preview","unknown"}) {
            env.setProperty("GEMINI_SUMMARY_MODEL",model);assertThat(settings.configured()).isFalse();
        }
        env.setProperty("GEMINI_SUMMARY_MODEL","gemini-3.1-flash-lite");
        for(var entry:java.util.Map.of("AI_SUMMARY_TIMEOUT_SECONDS","40","AI_SUMMARY_MAX_ATTEMPTS","3","AI_SUMMARY_MAX_CALLS_PER_MINUTE","3","AI_SUMMARY_MAX_CALLS_PER_DAY","20").entrySet()) {
            var limit=entry.getKey();
            env.setProperty(limit,"bad");assertThat(settings.configured()).isFalse();
            env.setProperty(limit,"0");assertThat(settings.configured()).isFalse();
            env.setProperty(limit,entry.getValue());assertThat(settings.configured()).isTrue();
        }
        env.setProperty("GEMINI_API_KEY","bad\nheader");assertThat(settings.configured()).isFalse();
    }
    @Test void fallbackMustBeExplicitAndLimitsCannotExceedDesign() {
        var env=new MockEnvironment().withProperty("AI_SUMMARY_ENABLED","true").withProperty("GEMINI_API_KEY","fake");
        var settings=new SummarySettings(env);assertThat(settings.fallbackModel()).isEmpty();
        env.setProperty("GEMINI_SUMMARY_FALLBACK_MODEL","gemini-3.5-flash-lite");assertThat(settings.configured()).isTrue();
        env.setProperty("GEMINI_SUMMARY_FALLBACK_MODEL","latest");assertThat(settings.configured()).isFalse();
        env.setProperty("GEMINI_SUMMARY_FALLBACK_MODEL","");env.setProperty("AI_SUMMARY_TIMEOUT_SECONDS","41");assertThat(settings.configured()).isFalse();
    }
}
