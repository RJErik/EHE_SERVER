// File: src/main/java/com/example/ehe_server/exception/custom/InvalidAlertThresholdException.java
package com.example.ehe_server.exception.custom;

/**
 * Thrown when an alert is being created with a threshold value that is null or not greater than zero.
 * Extends BusinessRuleException (HTTP 409).
 */
public class InvalidAlertThresholdException extends BusinessRuleException {
    public InvalidAlertThresholdException(Object thresholdValue) {
        super("error.message.INVALID_THRESHOLD_VALUE", "error.logDetail.INVALID_THRESHOLD_VALUE", thresholdValue);
    }
}