package com.example.ehe_server.exception.custom;

import java.math.BigDecimal;

public class InvalidThresholdValueException extends ValidationException {
    public InvalidThresholdValueException(BigDecimal value) {
        super("error.message.invalidThresholdValue", "error.logDetail.invalidThresholdValue", value);
    }
}