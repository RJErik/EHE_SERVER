package ehe_server.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotNullFieldValidator implements ConstraintValidator<NotNullField, Object> {

    private ValidationMetadata metadata;

    @Override
    public void initialize(NotNullField annotation) {
        this.metadata = new ValidationMetadata(
                annotation.exception().getName(),
                annotation.params(),
                annotation.actionLinkText(),
                annotation.actionLinkTarget(),
                annotation.showResendButton()
        );
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value != null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(metadata.toJson())
                .addConstraintViolation();

        return false;
    }
}