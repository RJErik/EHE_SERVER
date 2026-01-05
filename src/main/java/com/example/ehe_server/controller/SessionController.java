package com.example.ehe_server.controller;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.properties.FrontendProperties;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.session.LogInServiceInterface;
import com.example.ehe_server.service.intf.session.UserLogoutServiceInterface;
import com.example.ehe_server.service.intf.session.UserValidationServiceInterface;
import com.example.ehe_server.service.intf.session.JwtTokenRenewalServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    private final LogInServiceInterface logInService;
    private final MessageSource messageSource;
    private final FrontendProperties frontendProperties;

    public SessionController(
            UserValidationServiceInterface userValidationService,
            UserLogoutServiceInterface userLogoutService,
            UserContextServiceInterface userContextService,
            JwtTokenRenewalServiceInterface jwtTokenRenewalService,
            LogInServiceInterface logInService,
            MessageSource messageSource,
            FrontendProperties frontendProperties) {
        this.userValidationService = userValidationService;
        this.userLogoutService = userLogoutService;
        this.userContextService = userContextService;
        this.jwtTokenRenewalService = jwtTokenRenewalService;
        this.messageSource = messageSource;
        this.frontendProperties = frontendProperties;
        this.logInService = logInService;
    }

    /**
     * GET /api/session
     * Verify/retrieve current session status.
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

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/session
     * Authenticate user and create session.
     */
    @PostMapping()
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        logInService.authenticateUser(request.getEmail(), request.getPassword(), response);

        String successMessage = messageSource.getMessage(
                "success.message.auth.login",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        if (userContextService.getCurrentUserRole().contains("ROLE_ADMIN")) {
            responseBody.put("redirectUrl", frontendProperties.getUrl() + frontendProperties.getAdmin());
        } else {
            String fullUrl = frontendProperties.getUrl() + frontendProperties.getUser().getSuffix();
            responseBody.put("redirectUrl", fullUrl);
        }

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/session/token
     * Create/refresh a new JWT token.
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

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}