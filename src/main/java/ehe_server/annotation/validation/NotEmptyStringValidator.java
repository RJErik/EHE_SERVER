package ehe_server.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotEmptyStringValidator implements ConstraintValidator<NotEmptyString, String> {

    private ValidationMetadata metadata;

    @Override
    public void initialize(NotEmptyString annotation) {
        this.metadata = new ValidationMetadata(
                annotation.exception().getName(),
                annotation.params(),
                annotation.actionLinkText(),
                annotation.actionLinkTarget(),
                annotation.showResendButton()
        );
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value != null && !value.trim().isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(metadata.toJson())
                .addConstraintViolation();

        return false;
    }
}