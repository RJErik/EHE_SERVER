// src/main/java/com/example/ehe_server/service/intf/auth/RegistrationServiceInterface.java
package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.dto.RegistrationRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface RegistrationServiceInterface {
    Map<String, Object> registerUser(RegistrationRequest request, HttpServletResponse response);
}
