package com.example.ehe_server.service.intf.auth;

public interface HashingServiceInterface {
    /**
     * Hash an email address using SHA-256
     * @param email The email to hash
     * @return The hexadecimal string representation of the hash
     */
    String hashEmail(String email);
}
