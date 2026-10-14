package com.ztech.crm.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CuitValidator implements ConstraintValidator<ValidCuit, String> {

    private static final Pattern CUIT_PATTERN = Pattern.compile("^(?:[0-9]{11}|[0-9]{2}-[0-9]{8}-[0-9])$");
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // El campo es opcional (DP-03); @NotBlank se ocuparía de la obligatoriedad
            // si en algún punto pasara a serlo.
            return true;
        }
        String stripped = value.strip();
        if (!CUIT_PATTERN.matcher(stripped).matches()) return false;
        String digits = stripped.replace("-", "");
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        int checkDigit = (11 - sum % 11) % 11;
        return checkDigit == digits.charAt(10) - '0';
    }
}
