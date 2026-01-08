package ehe_server.annotation.validation;

import ehe_server.exception.custom.CustomBaseException;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PositiveAmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PositiveAmount {

    /**
     * The exception class that contains messageKey and logDetailKey.
     */
    Class<? extends CustomBaseException> exception();

    /**
     * Parameters for message formatting.
     */
    String[] params() default {};

    String actionLinkText() default "";
    String actionLinkTarget() default "";
    boolean showResendButton() default false;

    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}