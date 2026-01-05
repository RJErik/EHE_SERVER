package com.example.ehe_server.exception.custom;

public class AdminModificationForbiddenException extends ResourceNotFoundException {
    public AdminModificationForbiddenException(Integer targetUserId) {
        super("error.message.userNotFound", "error.logDetail.adminModificationForbidden", targetUserId);
    }
}