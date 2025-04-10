package com.example.ehe_server.service.auth;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.LogInServiceInterface;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.HashingServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class LogInService implements LogInServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final HashingServiceInterface hashingService;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public LogInService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        JwtTokenGeneratorInterface jwtTokenGenerator,
                        HashingServiceInterface hashingService,
                        CookieServiceInterface cookieService,
                        LoggingServiceInterface loggingService,
                        AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.hashingService = hashingService;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> authenticateUser(LoginRequest request, HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Validate both fields are provided
            if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
                    (request.getPassword() == null || request.getPassword().trim().isEmpty())) {
                responseBody.put("success", false);
                responseBody.put("message", "Please enter both email and password");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Login failed: Missing email and password");
                return responseBody;
            }

            // Validate email is provided
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                responseBody.put("success", false);
                responseBody.put("message", "Please enter your email address");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Login failed: Missing email");
                return responseBody;
            }

            // Validate password is provided
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                responseBody.put("success", false);
                responseBody.put("message", "Please enter your password");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Login failed: Missing password");
                return responseBody;
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                responseBody.put("success", false);
                responseBody.put("message", "Please enter a valid email address");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Login failed: Invalid email format");
                return responseBody;
            }

            // Hash the email for lookup
            String emailHash = hashingService.hashEmail(request.getEmail());

            // Find user by email hash
            Optional<User> userOpt = userRepository.findByEmailHash(emailHash);

            if (userOpt.isEmpty()) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password. Try ");
                Map<String, String> actionLink = new HashMap<>();
                actionLink.put("text", "registering.");
                actionLink.put("target", "register");
                responseBody.put("actionLink", actionLink);
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Login failed: User not found for email " + request.getEmail());
                return responseBody;
            }

            User user = userOpt.get();
            auditContextService.setCurrentUser(String.valueOf(user.getUserId()));

            // Check account status

            if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
                responseBody.put("success", false);
                responseBody.put("message", "Your account is not verified. Try verifying it.");
                responseBody.put("showResendButton", true);
                loggingService.logAction(Integer.parseInt(auditContextService.getCurrentUser()), auditContextService.getCurrentUser(), "Login failed: Account is not verified.");
                return responseBody;
            }

            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                responseBody.put("success", false);
                responseBody.put("message", "Your account is not active");
                loggingService.logAction(Integer.parseInt(auditContextService.getCurrentUser()), auditContextService.getCurrentUser(), "Login failed: Account not active");
                return responseBody;
            }

            // Verify password
            if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password");
                loggingService.logAction(Integer.parseInt(auditContextService.getCurrentUser()), auditContextService.getCurrentUser(), "Login failed: Invalid password");
                return responseBody;
            }

            // Now we know the user, set the user context even though not authenticated yet
            auditContextService.setCurrentUser(String.valueOf(user.getUserId()));

            // Check if user is an admin
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Create roles list based on user status
            List<String> roles = new ArrayList<>();
            roles.add("USER"); // All authenticated users have USER role

            if (isAdmin) {
                roles.add("ADMIN"); // Add ADMIN role if user is in Admin table
            }

            // Update audit context with authenticated user and roles
            String rolesString = String.join(",", roles);
            auditContextService.setCurrentUserRole(rolesString);

            // Generate JWT token with user ID and roles
            String jwtToken = jwtTokenGenerator.generateToken(Long.valueOf(user.getUserId()), roles);

            // Create JWT cookie
            cookieService.createJwtCookie(jwtToken, response);

            // Log successful login
            loggingService.logAction(user.getUserId(), String.valueOf(user.getUserId()), "Login successful");

            // Return success response
            responseBody.put("success", true);
            responseBody.put("message", "Login successful");
            responseBody.put("userName", user.getUserName());
            responseBody.put("roles", roles);

            return responseBody;

        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("message", "An error occurred during login");

            // Log the error
            loggingService.logError(null,auditContextService.getCurrentUser(), "Error during login: " + e.getMessage(), e);

            return responseBody;
        }
    }

    @Override
    public void logoutUser(HttpServletResponse response) {
        // Get the current user context before logout
        String userId = auditContextService.getCurrentUser();

        // Log the logout action
        Integer userIdInt = null;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            // userId was not a valid number, leave userIdInt as null
        }

        loggingService.logAction(userIdInt, userId, "User logged out");

        // Clear JWT cookie
        cookieService.clearJwtCookie(response);

        // Reset user context
        auditContextService.setCurrentUser("unauthenticated");
        auditContextService.setCurrentUserRole("none");
    }
}
