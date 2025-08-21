package com.example.ehe_server.exception.custom;

public class InvalidConditionTypeException extends ValidationException {
    public InvalidConditionTypeException(String type) {
        super("error.message.invalidConditionType", "error.logDetail.invalidConditionType", type);
    }
}