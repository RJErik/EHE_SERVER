package com.example.ehe_server.service.intf.auth;

import java.util.Map;

public interface PasswordResetRequestServiceInterface {
    Map<String, Object> requestPasswordReset(String email);
}
