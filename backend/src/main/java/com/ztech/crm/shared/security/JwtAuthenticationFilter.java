package com.ztech.crm.shared.security;

import com.ztech.crm.access.domain.User;
import com.ztech.crm.access.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida los claims del JWT y contrasta {@code authVersion}, rol y estado con el
 * usuario actual para revocar tokens anteriores después de cambios sensibles. Luego
 * deja el usuario en el {@code SecurityContext} (BE-SEC-02/03). De paso agrega
 * {@code tenantId}/{@code userId} al MDC (docs/arquitectura/01-arquitectura-general.md:
 * "los logs incluyen requestId, userId y tenantId") — se limpia siempre en el
 * {@code finally}, porque Tomcat reutiliza hilos entre requests distintos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final boolean enforcePasswordChange;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   @Value("${app.security.enforce-password-change:true}")
                                   boolean enforcePasswordChange) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.enforcePasswordChange = enforcePasswordChange;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        boolean authenticated = false;

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            AuthenticatedUser tokenUser;
            try {
                tokenUser = jwtService.parseToken(header.substring(BEARER_PREFIX.length()));
            } catch (InvalidTokenException ex) {
                RestAuthenticationEntryPoint.writeProblem(response, request, HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED", "Credenciales inválidas o token vencido.");
                return;
            }

            User persistedUser = userRepository.findByIdAndTenantId(tokenUser.userId(), tokenUser.tenantId())
                    .filter(User::isActive)
                    .filter(candidate -> candidate.getAuthVersion() == tokenUser.authVersion())
                    .orElse(null);
            if (persistedUser == null || persistedUser.getRole() != tokenUser.role()) {
                RestAuthenticationEntryPoint.writeProblem(response, request, HttpStatus.UNAUTHORIZED,
                        "TOKEN_REVOKED", "La sesión dejó de ser válida.");
                return;
            }

            AuthenticatedUser user = new AuthenticatedUser(
                    persistedUser.getId(), persistedUser.getTenantId(), persistedUser.getEmail(), null,
                    persistedUser.getRole(), persistedUser.isActive(), persistedUser.mustChangePassword(),
                    persistedUser.getAuthVersion());

            if (enforcePasswordChange && user.mustChangePassword()
                    && !request.getRequestURI().equals("/api/v1/auth/change-password")) {
                RestAuthenticationEntryPoint.writeProblem(response, request, HttpStatus.FORBIDDEN,
                        "PASSWORD_CHANGE_REQUIRED", "Debe cambiar la contraseña antes de continuar.");
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            MDC.put(MDC_TENANT_ID, String.valueOf(user.tenantId()));
            MDC.put(MDC_USER_ID, String.valueOf(user.userId()));
            authenticated = true;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (authenticated) {
                MDC.remove(MDC_TENANT_ID);
                MDC.remove(MDC_USER_ID);
            }
        }
    }
}
