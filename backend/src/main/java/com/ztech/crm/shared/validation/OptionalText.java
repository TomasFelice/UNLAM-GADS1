package com.ztech.crm.shared.validation;

/** Normalización previa a Bean Validation para campos opcionales de solicitudes. */
public final class OptionalText {
    private OptionalText() { }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static String canonicalCuit(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.replace("-", "");
    }
}
