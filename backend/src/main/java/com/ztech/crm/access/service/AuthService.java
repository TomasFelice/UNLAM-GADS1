package com.ztech.crm.access.service;

import com.ztech.crm.access.domain.User;
import com.ztech.crm.access.dto.request.LoginRequest;
import com.ztech.crm.access.dto.response.LoginResponse;
import com.ztech.crm.access.repository.UserRepository;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de login (BE-SEC-01). La verificación de credenciales la hace
 * {@code AuthenticationManager} (vía {@code CustomUserDetailsService} +
 * {@code PasswordEncoder}, auto-configurados por Spring Security a partir de esos dos
 * beans); acá sólo se orquesta y se emite el token.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                        UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        String token = jwtService.generateToken(authenticatedUser);

        // Los datos de presentación (nombre/apellido) no viajan en el JWT —
        // se consultan una vez, sólo para armar la respuesta del login.
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado sin registro correspondiente."));

        LoginResponse.UserSummary summary = new LoginResponse.UserSummary(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(),
                user.mustChangePassword(), summary);
    }
}
