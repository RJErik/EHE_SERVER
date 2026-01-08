package ehe_server.controller;

import ehe_server.annotation.validation.MinValue;
import ehe_server.annotation.validation.NotNullField;
import com.example.ehe_server.dto.*;
import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.UserResponse;
import ehe_server.dto.UserSearchRequest;
import ehe_server.dto.UserUpdateRequest;
import ehe_server.exception.custom.InvalidPageNumberException;
import ehe_server.exception.custom.InvalidPageSizeException;
import ehe_server.exception.custom.MissingPageNumberException;
import ehe_server.exception.custom.MissingPageSizeException;
import ehe_server.service.intf.user.UserRetrievalServiceInterface;
import ehe_server.service.intf.user.UserSearchServiceInterface;
import ehe_server.service.intf.user.UserUpdateServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@Validated
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
     * GET /api/admin/users?size=20&page=0
     * Retrieve all users with pagination via query parameters.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsers(
            @NotNullField(exception = MissingPageSizeException.class)
            @MinValue(exception = InvalidPageSizeException.class, min = 1)
            @RequestParam() Integer size,
            @NotNullField(exception = MissingPageNumberException.class)
            @MinValue(exception = InvalidPageNumberException.class, min = 0)
            @RequestParam() Integer page) {

        PaginatedResponse<UserResponse> userRetrievalResponses =
                userRetrievalService.getUsers(size, page);

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
     * POST /api/admin/users/search
     * Search users with filters via request body (POST to keep PII like email out of URLs).
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @Valid @RequestBody UserSearchRequest request) {

        PaginatedResponse<UserResponse> userSearchResponses =
                userSearchService.searchUsers(
                        request.getUserId(),
                        request.getUserName(),
                        request.getEmail(),
                        request.getAccountStatus(),
                        request.getRegistrationDateTo(),
                        request.getRegistrationDateFrom(),
                        request.getSize(),
                        request.getPage()
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

    /**
     * PUT /api/admin/users/{userId}
     * Update an existing user (full update)
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse userUpdateResponse = userUpdateService.updateUserInfo(
                userId,
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