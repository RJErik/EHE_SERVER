package com.example.ehe_server.exception.custom;

public class UserEmailNotFoundException extends ResourceNotFoundException {
    public UserEmailNotFoundException(String email) {
        super("error.message.invalidEmail", "error.logDetail.userEmailNotFound", email);
    }
}