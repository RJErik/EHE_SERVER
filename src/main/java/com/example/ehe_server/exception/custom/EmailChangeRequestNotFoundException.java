package com.example.ehe_server.exception.custom;

public class EmailChangeRequestNotFoundException extends ResourceNotFoundException {
    public EmailChangeRequestNotFoundException(String token) {
        super("error.message.emailChangeRequestNotFound", "error.logDetail.emailChangeRequestNotFound", token);
    }
}