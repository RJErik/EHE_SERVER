package com.example.ehe_server.controller;

import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.user.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final UserValidationServiceInterface userValidationService;
    private final UserLogoutServiceInterface userLogoutService;
    private final UserContextServiceInterface userContextService;
    private final JwtTokenRenewalServiceInterface jwtTokenRenewalService;
    private final MessageSource messageSource;

    public SessionController(
            UserValidationServiceInterface userValidationService,
            UserLogoutServiceInterface userLogoutService,
            UserContextServiceInterface userContextService,
            JwtTokenRenewalServiceInterface jwtTokenRenewalService,
            MessageSource messageSource) {
        this.userValidationService = userValidationService;
        this.userLogoutService = userLogoutService;
        this.userContextService = userContextService;
        this.jwtTokenRenewalService = jwtTokenRenewalService;
        this.messageSource = messageSource;
    }

    @GetMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser() {
        // Call automated trade rule retrieval service
        userValidationService.verifyUser();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.verifyUser", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        // Call automated trade rule retrieval service
        userLogoutService.logoutUser(userContextService.getCurrentUserId(), request, response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.logout", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/renew-token")
    public ResponseEntity<Map<String, Object>> renewToken(HttpServletRequest request, HttpServletResponse response) {
        // Call automated trade rule retrieval service
        jwtTokenRenewalService.renewToken(userContextService.getCurrentUserId(), request, response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.jwtTokenRenewal", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}
