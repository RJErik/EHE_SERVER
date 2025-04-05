package com.example.ehe_server.service.intf.email;

import com.example.ehe_server.entity.User;

public interface EmailServiceInterface {

    /**
     * Sends a verification email to the newly registered user.
     * @param user The user who registered.
     * @param token The verification token.
     * @param recipientEmail The email address to send to (passed explicitly for decoupling).
     */
    void sendVerificationEmail(User user, String token, String recipientEmail);

    /**
     * Sends a password reset email.
     * Implement this later when needed.
     * @param user The user requesting the reset.
     * @param token The password reset token.
     * @param recipientEmail The email address to send to.
     */
    // void sendPasswordResetEmail(User user, String token, String recipientEmail);

    /**
     * Sends a generic informational email.
     * @param to The recipient email address.
     * @param subject The email subject.
     * @param text The email body content.
     */
    void sendSimpleMessage(String to, String subject, String text);
}