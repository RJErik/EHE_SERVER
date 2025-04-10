package com.example.ehe_server.service.intf.auth;

import java.util.Map;

public interface RegistrationVerificationServiceInterface {

    Map<String, Object> verifyRegistrationToken(String token);
}