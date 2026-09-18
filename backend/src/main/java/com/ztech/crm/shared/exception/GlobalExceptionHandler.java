package com.ztech.crm.shared.exception;

import com.ztech.crm.shared.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce toda excepción a un {@link ProblemDetail} uniforme (RFC 7807, design.md §7).
 * Nunca expone stacktrace, ni distingue en el mensaje "no existe" de "no autorizado"
 * entre tenants (BE-SEC-04/10): ambos casos usan {@link NotFoundException}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return problemDetail(ex.getStatus(), ex.getMessage(), ex.getCode(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return problemDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.",
                "VALIDATION_ERROR", request, errors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.", "UNAUTHENTICATED", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación.", "FORBIDDEN", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Última línea de defensa ante una restricción de base violada (unicidad,
        // exclusión GiST de reservas — design.md §9.3). El detalle específico lo debería
        // anticipar el service con una ConflictException; esto cubre lo que se escape.
        log.warn("Violación de integridad no anticipada por el service: {}", ex.getMessage());
        return problemDetail(HttpStatus.CONFLICT, "El registro entra en conflicto con datos existentes.",
                "DATA_INTEGRITY_VIOLATION", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        // Varias excepciones propias de Spring MVC (ruta inexistente, método no
        // soportado, media type no soportado, etc.) implementan ErrorResponse y ya
        // traen su status correcto — no son un error interno real, así que no se
        // aplastan a 500 ni se loguean como inesperadas.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return problemDetail(status, errorResponse.getBody().getDetail(), "REQUEST_ERROR", request, null);
        }

        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.",
                "INTERNAL_ERROR", request, null);
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail, String code,
                                         HttpServletRequest request, List<FieldValidationError> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("requestId", request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        problemDetail.setProperty("errors", errors == null ? List.of() : errors);
        return problemDetail;
    }

    private FieldValidationError formatFieldError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    public record FieldValidationError(String field, String message) {
    }
}
