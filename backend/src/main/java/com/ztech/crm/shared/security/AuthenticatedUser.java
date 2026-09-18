package com.ztech.crm.shared.security;

import com.ztech.crm.access.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador del usuario autenticado para Spring Security. No es la entidad JPA
 * {@code User} — nunca se expone directo (design.md regla de capas #5).
 * {@code passwordHash} sólo se completa al construirlo desde la base para el paso de
 * login ({@code CustomUserDetailsService}), donde {@code DaoAuthenticationProvider} lo
 * necesita para comparar contra la contraseña recibida. En el resto de los requests
 * este objeto se reconstruye desde los claims del JWT ({@link JwtAuthenticationFilter}),
 * ya autenticados, sin volver a consultar la base — ahí viaja con
 * {@code passwordHash = null}, porque el hash nunca se incluye en el token y no vuelve
 * a hacer falta.
 */
public record AuthenticatedUser(
        Long userId,
        Long tenantId,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        boolean mustChangePassword,
        long authVersion
) implements UserDetails, HasUserId {

    public AuthenticatedUser(Long userId, Long tenantId, String email, String passwordHash,
                             Role role, boolean active) {
        this(userId, tenantId, email, passwordHash, role, active, false, 0);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public Long getUserId() {
        return userId;
    }
}
