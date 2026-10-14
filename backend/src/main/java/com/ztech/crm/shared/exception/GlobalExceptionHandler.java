package com.ztech.crm.shared.exception;

import com.ztech.crm.shared.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos con un formato inválido. Revisá los valores ingresados.",
                "INVALID_REQUEST_BODY", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidParameter(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene un parámetro con formato inválido.",
                "INVALID_REQUEST_PARAMETER", request, null);
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
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                    && "no_overlapping_won_reservation".equals(constraint.getConstraintName())
                    || cause instanceof java.sql.SQLException sql && "23P01".equals(sql.getSQLState())
                    && sql.getMessage() != null && sql.getMessage().contains("\"no_overlapping_won_reservation\"")) {
                return problemDetail(HttpStatus.CONFLICT,
                        "El salón ya tiene una reserva en ese horario. Elegí otro horario o salón.",
                        "VENUE_ALREADY_BOOKED", request, null);
            }
        }
        return problemDetail(HttpStatus.CONFLICT, "El registro entra en conflicto con datos existentes.",
                "DATA_INTEGRITY_VIOLATION", request, null);
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail handleOptimisticLock(Exception ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.CONFLICT,
                "La oportunidad cambió desde que la abriste. Recargá los datos antes de volver a guardar.",
                "OPPORTUNITY_VERSION_CONFLICT", request, null);
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentTransaction(Exception ex, HttpServletRequest request) {
        return problemDetail(HttpStatus.CONFLICT,
                "Otra operación cambió los datos o la disponibilidad durante la confirmación. Recargá antes de reintentar.",
                "CONCURRENT_WRITE_CONFLICT", request, null);
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
            String detail = switch (status) {
                case NOT_FOUND -> "No se encontró el recurso solicitado.";
                case METHOD_NOT_ALLOWED -> "La operación no está disponible para este recurso.";
                case UNSUPPORTED_MEDIA_TYPE -> "Enviá la solicitud en formato JSON.";
                case NOT_ACCEPTABLE -> "El formato de respuesta solicitado no está disponible.";
                case BAD_REQUEST -> "La solicitud contiene datos inválidos.";
                default -> "No se pudo completar la solicitud.";
            };
            return problemDetail(status, detail, "REQUEST_ERROR", request, null);
        }

        log.error("Error no controlado en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.",
                "INTERNAL_ERROR", request, null);
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail, String code,
                                         HttpServletRequest request, List<FieldValidationError> errors) {
        log.warn("Solicitud rechazada status={} code={}", status.value(), code);
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
