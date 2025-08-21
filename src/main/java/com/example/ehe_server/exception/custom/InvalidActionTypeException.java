package com.example.ehe_server.exception.custom;

public class InvalidActionTypeException extends ValidationException {
    public InvalidActionTypeException(String type) {
        super("error.message.invalidActionType", "error.logDetail.invalidActionType", type);
    }
}