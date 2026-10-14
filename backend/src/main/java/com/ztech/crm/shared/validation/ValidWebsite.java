package com.ztech.crm.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Sitio web opcional con URL absoluta HTTP/HTTPS. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WebsiteValidator.class)
public @interface ValidWebsite {
    String message() default "El sitio web debe ser una URL completa HTTP o HTTPS.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
