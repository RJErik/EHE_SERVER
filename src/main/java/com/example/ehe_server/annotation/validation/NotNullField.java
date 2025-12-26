package com.example.ehe_server.annotation.validation;

import com.example.ehe_server.exception.custom.CustomBaseException;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotNullFieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNullField {

    /**
     * The exception class that contains messageKey and logDetailKey.
     * Must have a no-arg constructor.
     */
    Class<? extends CustomBaseException> exception();

    /**
     * Parameters for message formatting.
     * Use "$value" placeholder to insert the rejected value.
     * Example: params = {"$value"} → {0} = rejected value
     * Example: params = {"context", "$value"} → {0} = "context", {1} = rejected value
     */
    String[] params() default {};

    /**
     * Action link text (e.g., "forgot password", "log in").
     */
    String actionLinkText() default "";

    /**
     * Action link target/route (e.g., "forgot-password", "login").
     */
    String actionLinkTarget() default "";

    /**
     * Whether to show a resend button in the response.
     */
    boolean showResendButton() default false;

    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}