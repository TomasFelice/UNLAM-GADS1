package com.ztech.crm.assistance.service;

/** Sólo códigos controlados; nunca retiene cuerpos ni causas upstream. */
public final class ProviderFailure extends RuntimeException {
    public enum Kind { TRANSIENT, TEMPORARY_RATE, QUOTA, BLOCKED, INVALID, UNAVAILABLE, TIMEOUT }
    private final Kind kind;
    private final Long retryAfterSeconds;
    public ProviderFailure(Kind kind,Long retryAfterSeconds) {super(kind.name());this.kind=kind;this.retryAfterSeconds=retryAfterSeconds;}
    public Kind kind(){return kind;}
    public Long retryAfterSeconds(){return retryAfterSeconds;}
}
