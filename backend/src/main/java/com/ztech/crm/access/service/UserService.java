package com.ztech.crm.access.service;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.access.domain.User;
import com.ztech.crm.access.dto.request.ChangePasswordRequest;
import com.ztech.crm.access.dto.request.CreateUserRequest;
import com.ztech.crm.access.dto.request.ResetPasswordRequest;
import com.ztech.crm.access.dto.request.UpdateUserRequest;
import com.ztech.crm.access.dto.response.UserResponse;
import com.ztech.crm.access.dto.response.UserSummaryResponse;
import com.ztech.crm.access.repository.UserRepository;
import com.ztech.crm.access.repository.specification.UserSpecifications;
import com.ztech.crm.shared.dto.PageResponse;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.ForbiddenException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import com.ztech.crm.shared.security.TenantContext;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String q, Role role, Boolean active, Pageable pageable) {
        Specification<User> filters = UserSpecifications.tenant(TenantContext.currentTenantId());
        if (q != null && !q.isBlank()) {
            filters = filters.and(UserSpecifications.contains(q));
        }
        if (role != null) {
            filters = filters.and(UserSpecifications.role(role));
        }
        if (active != null) {
            filters = filters.and(UserSpecifications.active(active));
        }
        return PageResponse.from(userRepository.findAll(filters, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAssignable() {
        AuthenticatedUser current = TenantContext.currentUser();
        Specification<User> filters = UserSpecifications.tenant(current.tenantId())
                .and(UserSpecifications.active(true));
        if (current.role() == Role.SELLER) {
            return userRepository.findByIdAndTenantId(current.userId(), current.tenantId())
                    .map(this::toResponse).stream().toList();
        }
        return userRepository.findAll(filters).stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(getOwnedOrThrow(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        assertUniqueEmail(email, null);
        User user = new User(TenantContext.currentTenantId(), email,
                passwordEncoder.encode(request.temporaryPassword()), request.firstName().strip(),
                request.lastName().strip(), request.role());
        user.requirePasswordChange();
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AuthenticatedUser current = TenantContext.currentUser();
        User user = getOwnedOrThrow(id);
        if (current.userId().equals(id) && !request.active()) {
            throw new ForbiddenException("No puede desactivar su propio usuario.");
        }
        assertLastAdminRemains(user, request.role(), request.active());
        String email = normalizeEmail(request.email());
        assertUniqueEmail(email, id);
        user.updateProfile(email, request.firstName().strip(), request.lastName().strip(),
                request.role(), request.active());
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = getOwnedOrThrow(id);
        user.setTemporaryPassword(passwordEncoder.encode(request.temporaryPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        AuthenticatedUser current = TenantContext.currentUser();
        User user = getOwnedOrThrow(current.userId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("CURRENT_PASSWORD_INVALID", "La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("PASSWORD_UNCHANGED", "La nueva contraseña debe ser diferente de la actual.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void assertActiveAndAssignable(Long id) {
        User user = getOwnedOrThrow(id);
        if (!user.isActive()) {
            throw new BusinessException("USER_INACTIVE", "El responsable seleccionado no está activo.");
        }
    }

    /** Resuelve autoría histórica aun cuando el usuario ya no esté activo. */
    @Transactional(readOnly = true)
    public UserSummaryResponse getSummary(Long id) {
        User user = getOwnedOrThrow(id);
        return new UserSummaryResponse(user.getId(), user.getFirstName(), user.getLastName());
    }

    private void assertLastAdminRemains(User user, Role newRole, boolean newActive) {
        boolean removesActiveAdmin = user.getRole() == Role.ADMIN && user.isActive()
                && (newRole != Role.ADMIN || !newActive);
        if (removesActiveAdmin && userRepository.countByTenantIdAndRoleAndActiveTrue(
                TenantContext.currentTenantId(), Role.ADMIN) <= 1) {
            throw new ConflictException("LAST_ACTIVE_ADMIN", "Debe permanecer al menos un ADMIN activo.");
        }
    }

    private void assertUniqueEmail(String email, Long excludeId) {
        boolean exists = excludeId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
        if (exists) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Ya existe un usuario con ese email.");
        }
    }

    private User getOwnedOrThrow(Long id) {
        return userRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el usuario solicitado."));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.isActive(), user.mustChangePassword());
    }
}
