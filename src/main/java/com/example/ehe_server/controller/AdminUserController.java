package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.intf.user.UserRetrievalServiceInterface;
import com.example.ehe_server.service.intf.user.UserSearchServiceInterface;
import com.example.ehe_server.service.intf.user.UserUpdateServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final MessageSource messageSource;
    private final UserRetrievalServiceInterface userRetrievalService;
    private final UserSearchServiceInterface userSearchService;
    private final UserUpdateServiceInterface userUpdateService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public AdminUserController(UserRetrievalServiceInterface userRetrievalService,
                               UserSearchServiceInterface userSearchService,
                               MessageSource messageSource,
                               UserUpdateServiceInterface userUpdateService) {
        this.userRetrievalService = userRetrievalService;
        this.userSearchService = userSearchService;
        this.userUpdateService = userUpdateService;
        this.messageSource = messageSource;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers() {
        // Call automated trade rule retrieval service
        List<UserRetrievalResponse> userRetrievalResponses = userRetrievalService.getUsers();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("users", userRetrievalResponses);


        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/users/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestBody UserSearchRequest request) {
        // Parse date strings to LocalDateTime
        LocalDateTime registrationDateToTime = parseLocalDateTime(request.getRegistrationDateToTime());
        LocalDateTime registrationDateFromTime = parseLocalDateTime(request.getRegistrationDateFromTime());

        // Call transaction search service with extracted parameters
        List<UserSearchResponse> userSearchResponses = userSearchService.searchUsers(
                request.getUserId(),
                request.getUserName(),
                request.getEmail(),
                request.getAccountStatus(),
                registrationDateToTime,
                registrationDateFromTime
        );

        // Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.search",
                null,
                LocaleContextHolder.getLocale()
        );

        // Build the final response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("users", userSearchResponses);

        // Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/users/update")
    public ResponseEntity<Map<String, Object>> updateUsers(@RequestBody UserUpdateRequest request) {

        // Call transaction search service with extracted parameters
        UserUpdateResponse userUpdateResponse = userUpdateService.updateUserInfo(
                request.getUserId(),
                request.getUserName(),
                request.getEmail(),
                request.getPassword(),
                request.getAccountStatus()
        );

        // Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.update",
                null,
                LocaleContextHolder.getLocale()
        );

        // Build the final response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("user", userUpdateResponse);

        // Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Parse ISO format LocalDateTime string
     */
    private LocalDateTime parseLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
