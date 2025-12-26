package com.example.ehe_server.service.intf.email;

import com.example.ehe_server.entity.User;

public interface EmailSenderServiceInterface {
    void sendRegistrationVerificationEmail(User user, String token, String recipientEmail);
    void sendPasswordResetEmail(User user, String token, String recipientEmail);
    void sendEmailChangeVerificationEmail(User user, String token, String newEmail);
}