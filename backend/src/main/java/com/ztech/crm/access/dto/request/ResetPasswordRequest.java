package com.ztech.crm.access.dto.request;

import com.ztech.crm.shared.validation.ValidPassword;

public record ResetPasswordRequest(
        @ValidPassword
        String temporaryPassword
) {
}
