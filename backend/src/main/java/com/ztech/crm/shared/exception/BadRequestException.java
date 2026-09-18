package com.ztech.crm.shared.exception;

import org.springframework.http.HttpStatus;

/** Parámetros HTTP inválidos que no llegan a Bean Validation (paginación y orden). */
public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
