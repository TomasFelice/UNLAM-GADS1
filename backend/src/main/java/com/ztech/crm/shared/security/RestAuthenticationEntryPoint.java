package com.ztech.crm.shared.security;

import com.ztech.crm.shared.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Entra en juego cuando el request nunca llega a un controller — token ausente,
 * inválido o vencido, rechazado por la cadena de filtros de Spring Security. En ese
 * punto {@code @RestControllerAdvice} (GlobalExceptionHandler) no alcanza a
 * interceptar, porque el {@code DispatcherServlet} todavía no entró en juego
 * (design.md §7); por eso el mismo formato de {@code ProblemDetail} se arma acá a mano,
 * sin depender de un {@code ObjectMapper} inyectado (Spring Boot 4 usa Jackson 3, con
 * una API distinta a la que veníamos usando — se evita esa incertidumbre en un punto
 * tan temprano y crítico del pipeline).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        writeProblem(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                "Credenciales inválidas o token vencido.");
    }

    static void writeProblem(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                              String code, String detail) throws IOException {
        log.warn("Acceso rechazado status={} code={}", status.value(), code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String json = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s","requestId":"%s","errors":[]}"""
                .formatted(status.getReasonPhrase(), status.value(), escape(detail), code,
                        requestId == null ? "" : requestId.toString());
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
