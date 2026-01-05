package com.example.ehe_server.exception.custom;

public class InvalidEmailFormatException extends ValidationException {
    public InvalidEmailFormatException(String email) {
        super("error.message.invalidEmailFormat", "error.logDetail.invalidEmailFormat", email);
    }

    public InvalidEmailFormatException() {
        super("error.message.invalidEmailFormat", "error.logDetail.invalidEmailFormat");
    }
}