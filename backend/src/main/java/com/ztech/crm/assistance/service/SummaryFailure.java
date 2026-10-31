package com.ztech.crm.assistance.service;

import com.ztech.crm.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SummaryFailure extends ApiException {
    private final HttpStatus status;
    private final Long retryAfterSeconds;
    public SummaryFailure(HttpStatus status,String code,String message) {this(status,code,message,null);}
    public SummaryFailure(HttpStatus status,String code,String message,Long retryAfterSeconds) {
        super(code,message);this.status=status;this.retryAfterSeconds=retryAfterSeconds;
    }
    @Override public HttpStatus getStatus(){return status;}
    public Long retryAfterSeconds(){return retryAfterSeconds;}
    public static SummaryFailure invalid(){return new SummaryFailure(HttpStatus.BAD_GATEWAY,"AI_INVALID_RESPONSE","No se pudo validar el resumen recibido.");}
    public static SummaryFailure timeout(){return new SummaryFailure(HttpStatus.GATEWAY_TIMEOUT,"AI_TIMEOUT","El resumen tardó demasiado. Intentá nuevamente.");}
    public static SummaryFailure unavailable(){return new SummaryFailure(HttpStatus.SERVICE_UNAVAILABLE,"AI_UNAVAILABLE","El servicio de resumen no está disponible. Intentá más tarde.");}
}
