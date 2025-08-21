package com.example.ehe_server.exception.custom;

public class AccountSuspendedException extends AuthorizationException {
    public AccountSuspendedException(String email) {
        super("error.message.accountSuspended", "error.logDetail.accountSuspended", email);
    }
}