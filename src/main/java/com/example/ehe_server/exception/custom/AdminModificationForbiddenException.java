package com.example.ehe_server.exception.custom;

/**
 * Thrown when an admin attempts to modify another admin.
 * Extends ResourceNotFoundException to hide the existence of other admins from the requester.
 */
public class AdminModificationForbiddenException extends ResourceNotFoundException {
    public AdminModificationForbiddenException(Integer targetUserId) {
        super("error.message.userNotFound", "error.logDetail.adminModificationForbidden", targetUserId);
    }
}