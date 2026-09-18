package com.ztech.crm.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default "La contraseña debe tener entre 10 y 72 caracteres, mayúscula, minúscula, número y símbolo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
