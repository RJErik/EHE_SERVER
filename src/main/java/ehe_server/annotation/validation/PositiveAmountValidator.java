package ehe_server.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class PositiveAmountValidator implements ConstraintValidator<PositiveAmount, BigDecimal> {

    private ValidationMetadata metadata;

    @Override
    public void initialize(PositiveAmount annotation) {
        this.metadata = new ValidationMetadata(
                annotation.exception().getName(),
                annotation.params(),
                annotation.actionLinkText(),
                annotation.actionLinkTarget(),
                annotation.showResendButton()
        );
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(metadata.toJson())
                .addConstraintViolation();

        return false;
    }
}