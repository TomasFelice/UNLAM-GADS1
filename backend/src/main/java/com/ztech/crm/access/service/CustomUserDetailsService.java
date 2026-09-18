package com.ztech.crm.access.service;

import com.ztech.crm.access.domain.User;
import com.ztech.crm.access.repository.UserRepository;
import com.ztech.crm.shared.security.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre {@code User} (entidad JPA) y Spring Security. Sólo se usa durante el
 * login — {@code DaoAuthenticationProvider} llama a {@code loadUserByUsername} y
 * compara la contraseña recibida contra {@link AuthenticatedUser#getPassword()}.
 * "Usuario inexistente" y "contraseña incorrecta" llegan al mismo
 * {@code BadCredentialsException} (Spring Security oculta
 * {@link UsernameNotFoundException} por defecto) — BE-SEC-01/10.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
        return toAuthenticatedUser(user);
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive(),
                user.mustChangePassword(),
                user.getAuthVersion()
        );
    }
}
