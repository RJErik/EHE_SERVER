package com.example.ehe_server.exception.custom;

public class InvalidQuantityTypeException extends ValidationException {
    public InvalidQuantityTypeException(String type) {
        super("error.message.invalidQuantityType", "error.logDetail.invalidQuantityType", type);
    }
}