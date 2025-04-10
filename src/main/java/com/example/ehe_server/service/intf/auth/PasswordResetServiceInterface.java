package com.example.ehe_server.service.intf.auth;

import java.util.Map;

public interface PasswordResetServiceInterface {
    Map<String, Object> resetPassword(String token, String newPassword);
}
