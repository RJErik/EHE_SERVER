package ehe_server.annotation.validation;

import ehe_server.exception.custom.CustomBaseException;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotEmptyStringValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEmptyString {

    /**
     * The exception class that contains messageKey and logDetailKey.
     * Must have a no-arg constructor.
     */
    Class<? extends CustomBaseException> exception();

    /**
     * Parameters for message formatting.
     * Use "$value" placeholder to insert the rejected value.
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