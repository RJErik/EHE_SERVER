package com.example.ehe_server.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinValueValidator implements ConstraintValidator<MinValue, Integer> {

    private int minValue;
    private ValidationMetadata metadata;

    @Override
    public void initialize(MinValue annotation) {
        this.minValue = annotation.min();
        this.metadata = new ValidationMetadata(
                annotation.exception().getName(),
                annotation.params(),
                annotation.actionLinkText(),
                annotation.actionLinkTarget(),
                annotation.showResendButton()
        );
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value >= minValue) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(metadata.toJson())
                .addConstraintViolation();

        return false;
    }
}