package com.example.ehe_server.service.intf.email;

import com.example.ehe_server.entity.User;
import org.springframework.mail.MailException;

import java.util.Map;

public interface EmailServiceInterface {

    void sendVerificationEmail(User user, String token, String recipientEmail);

    // void sendPasswordResetEmail(User user, String token, String recipientEmail); // Implement later

    void sendSimpleMessage(String to, String subject, String text) throws MailException;

    Map<String, Object> resendVerificationEmail(User user, String email);

    // New method for password reset
    void sendPasswordResetEmail(User user, String token, String recipientEmail);
}