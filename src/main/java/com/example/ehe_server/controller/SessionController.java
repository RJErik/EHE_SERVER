package com.example.ehe_server.controller;

import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.session.UserLogoutServiceInterface;
import com.example.ehe_server.service.intf.session.UserValidationServiceInterface;
import com.example.ehe_server.service.intf.session.JwtTokenRenewalServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * GET /api/session
     * Verify/retrieve current session status.
     *
     * Changed from: GET /api/session/verify-user
     * Reason: "verify-user" is a verb. GET on the resource itself implies
     * "get current session state" which includes validation.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> verifyUser() {
        userValidationService.verifyUser();

        String successMessage = messageSource.getMessage(
                "success.message.session.verifyUser",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * DELETE /api/session
     * End/destroy the current session (logout).
     *
     * Changed from: POST /api/session/logout
     * Reason: Logout is "deleting" the session resource. DELETE is the
     * appropriate HTTP method for resource removal.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        userLogoutService.logoutUser(userContextService.getCurrentUserId(), request, response);

        String successMessage = messageSource.getMessage(
                "success.message.session.logout",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 200 OK with body, or could use 204 No Content without body
        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/session/token
     * Create/refresh a new JWT token.
     *
     * Changed from: POST /api/session/renew-token
     * Reason: "renew-token" contains a verb. POST to /token represents
     * "create a new token" which is what renewal does.
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> renewToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        jwtTokenRenewalService.renewToken(userContextService.getCurrentUserId(), request, response);

        String successMessage = messageSource.getMessage(
                "success.message.session.jwtTokenRenewal",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 201 Created is appropriate for token creation
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}