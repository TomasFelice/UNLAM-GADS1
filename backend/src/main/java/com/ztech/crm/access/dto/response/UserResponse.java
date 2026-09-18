package com.ztech.crm.access.dto.response;

import com.ztech.crm.access.domain.Role;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        boolean mustChangePassword
) {
}
