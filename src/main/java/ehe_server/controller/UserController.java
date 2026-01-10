package ehe_server.controller;

import ehe_server.dto.EmailChangeRequest;
import ehe_server.dto.UserInfoResponse;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import ehe_server.service.intf.user.EmailChangeRequestServiceInterface;
import ehe_server.service.intf.user.UserDeactivationServiceInterface;
import ehe_server.service.intf.user.UserInfoServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserContextServiceInterface userContextService;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final UserInfoServiceInterface userInfoService;
    private final UserDeactivationServiceInterface userDeactivationService;
    private final EmailChangeRequestServiceInterface emailChangeRequestService;
    private final MessageSource messageSource;

    public UserController(
            UserContextServiceInterface userContextService,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            UserInfoServiceInterface userInfoService,
            UserDeactivationServiceInterface userDeactivationService,
            EmailChangeRequestServiceInterface emailChangeRequestService,
            MessageSource messageSource) {
        this.userContextService = userContextService;
        this.passwordResetRequestService = passwordResetRequestService;
        this.userInfoService = userInfoService;
        this.userDeactivationService = userDeactivationService;
        this.emailChangeRequestService = emailChangeRequestService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/profile
     * Retrieve current user's profile information.
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        UserInfoResponse userInfoResponse = userInfoService.getUserInfo(
                userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.user.userInfo.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("userInfo", userInfoResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/password-reset-requests
     * Request a password reset email for the authenticated user.
     */
    @PostMapping("/password-reset-requests")
    public ResponseEntity<Map<String, Object>> requestPasswordReset() {
        passwordResetRequestService.requestPasswordResetForAuthenticatedUser(
                userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordResetRequest",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * DELETE /api/user/account
     * Deactivate (soft delete) the current user's account.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deactivateAccount() {
        userDeactivationService.deactivateUser(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.user.userDeactivation",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/email-change-requests
     * Request an email change (sends verification to new email).
     */
    @PostMapping("/email-change-requests")
    public ResponseEntity<Map<String, Object>> requestEmailChange(
            @Valid @RequestBody EmailChangeRequest request) {
        emailChangeRequestService.requestEmailChange(
                userContextService.getCurrentUserId(),
                request.getNewEmail());

        String successMessage = messageSource.getMessage(
                "success.message.user.emailChangeRequest",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("showResendButton", true);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}