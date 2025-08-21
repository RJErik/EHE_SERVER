package com.example.ehe_server.service.intf.email;

import com.example.ehe_server.entity.User;
import java.util.Map;

public interface EmailServiceInterface {
    void sendVerificationEmail(User user, String token, String recipientEmail);
    void sendSimpleMessage(String to, String subject, String text);
    void resendVerificationEmail(User user, String email);
    void sendPasswordResetEmail(User user, String token, String recipientEmail);
    // Add this new method
    void sendEmailChangeVerificationEmail(User user, String token, String newEmail);
}
