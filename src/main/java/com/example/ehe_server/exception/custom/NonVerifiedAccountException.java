package com.example.ehe_server.exception.custom;

import com.example.ehe_server.entity.User;

public class NonVerifiedAccountException extends AuthorizationException {
    public NonVerifiedAccountException(String email, String status) {
        super("error.message.nonVerifiedAccount", "error.logDetail.nonVerifiedAccount", email, status);
    }
}