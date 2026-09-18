package com.ztech.crm.shared.security;

import com.ztech.crm.access.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emisión y validación del JWT (DT-11). Sólo access token en E1 — sin refresh token.
 * Claims: {@code sub} (userId), {@code tenantId}, {@code role}, {@code email}, {@code active}.
 */
@Component
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ACTIVE = "active";
    private static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";
    private static final String CLAIM_AUTH_VERSION = "authVersion";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim(CLAIM_TENANT_ID, user.tenantId())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ACTIVE, user.active())
                .claim(CLAIM_MUST_CHANGE_PASSWORD, user.mustChangePassword())
                .claim(CLAIM_AUTH_VERSION, user.authVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token inválido o vencido.");
        }

        Long userId = Long.valueOf(claims.getSubject());
        Long tenantId = claims.get(CLAIM_TENANT_ID, Long.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        String email = claims.get(CLAIM_EMAIL, String.class);
        boolean active = Boolean.TRUE.equals(claims.get(CLAIM_ACTIVE, Boolean.class));
        boolean mustChangePassword = Boolean.TRUE.equals(claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class));
        Number authVersionClaim = claims.get(CLAIM_AUTH_VERSION, Number.class);
        long authVersion = authVersionClaim == null ? -1 : authVersionClaim.longValue();

        return new AuthenticatedUser(userId, tenantId, email, null, role, active,
                mustChangePassword, authVersion);
    }
}
