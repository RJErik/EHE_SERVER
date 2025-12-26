package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.intf.user.UserRetrievalServiceInterface;
import com.example.ehe_server.service.intf.user.UserSearchServiceInterface;
import com.example.ehe_server.service.intf.user.UserUpdateServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final MessageSource messageSource;
    private final UserRetrievalServiceInterface userRetrievalService;
    private final UserSearchServiceInterface userSearchService;
    private final UserUpdateServiceInterface userUpdateService;

    public AdminUserController(UserRetrievalServiceInterface userRetrievalService,
                               UserSearchServiceInterface userSearchService,
                               MessageSource messageSource,
                               UserUpdateServiceInterface userUpdateService) {
        this.userRetrievalService = userRetrievalService;
        this.userSearchService = userSearchService;
        this.userUpdateService = userUpdateService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/admin/users
     * Retrieve all users (no filters)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsers() {
        List<UserResponse> userRetrievalResponses = userRetrievalService.getUsers();

        String successMessage = messageSource.getMessage(
                "success.message.user.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("users", userRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/admin/users/search?userId=1&userName=john&accountStatus=ACTIVE...
     *
     * Search users with filters via query parameters.
     *
     * NOTE: This endpoint contains PII (email). If your security policy requires
     * keeping PII out of URLs/logs, you can keep this as POST instead.
     * See alternative method below.
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestBody UserSearchRequest request) {
        List<UserResponse> userSearchResponses = userSearchService.searchUsers(
                request.getUserId(),
                request.getUserName(),
                request.getEmail(),
                request.getAccountStatus(),
                request.getRegistrationDateTo(),
                request.getRegistrationDateFrom()
        );

        String successMessage = messageSource.getMessage(
                "success.message.user.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("users", userSearchResponses);

        return ResponseEntity.ok(responseBody);
    }

    /*
     * ALTERNATIVE: If you want to keep email out of URLs for security,
     * keeping POST for search is acceptable. Uncomment this and remove
     * the GET version above.
     *
     * POST /api/admin/users/search
     *
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestBody UserSearchRequest request) {
        // Same implementation as GET version
    }
    */

    /**
     * PUT /api/admin/users/{userId}
     * Update an existing user (full update)
     *
     * Use PUT for full replacement, PATCH for partial updates.
     * Since you're updating multiple fields, PUT is appropriate.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse userUpdateResponse = userUpdateService.updateUserInfo(
                userId,  // From path, not body
                request.getUserName(),
                request.getEmail(),
                request.getPassword(),
                request.getAccountStatus()
        );

        String successMessage = messageSource.getMessage(
                "success.message.user.update",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("user", userUpdateResponse);

        return ResponseEntity.ok(responseBody);
    }
}