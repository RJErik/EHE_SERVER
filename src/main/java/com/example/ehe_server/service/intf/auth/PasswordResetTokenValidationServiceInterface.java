package com.example.ehe_server.service.intf.auth;

import java.util.Map;

public interface PasswordResetTokenValidationServiceInterface {
    Map<String, Object> validatePasswordResetToken(String token);
}
