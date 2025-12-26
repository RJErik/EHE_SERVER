package com.example.ehe_server.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class RegexPatternValidator implements ConstraintValidator<RegexPattern, String> {

    private Pattern compiledPattern;
    private ValidationMetadata metadata;

    @Override
    public void initialize(RegexPattern annotation) {
        this.compiledPattern = Pattern.compile(annotation.pattern());
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
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        if (compiledPattern.matcher(value).matches()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(metadata.toJson())
                .addConstraintViolation();

        return false;
    }
}