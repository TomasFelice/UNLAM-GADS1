package com.ztech.crm.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CUIT opcional: estructura y dígito verificador módulo 11 (DP-03). */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CuitValidator.class)
public @interface ValidCuit {

    String message() default "El CUIT debe tener 11 dígitos y un dígito verificador válido (XX-XXXXXXXX-X).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
