package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.user.JwtTokenRenewalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public JwtTokenRenewalService(
            UserRepository userRepository,
            AdminRepository adminRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> renewToken(Long userId, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get the current user from the database
            Optional<User> userOptional = userRepository.findById(userId.intValue());

            // Check if the user exists
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userId.toString(), "Token renewal failed: User not found");
                return result;
            }

            User user = userOptional.get();

            // Check if the account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId.intValue(), userId.toString(),
                        "Token renewal failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get the user's role
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Create roles list based on user status
            String role = "USER"; // All authenticated users have USER role

            if (isAdmin) {
                role = "ADMIN"; // Add ADMIN role if user is in Admin table
            }

            // Update audit context with authenticated user and roles
            auditContextService.setCurrentUserRole(role);

            // Generate a new token
            String newToken = jwtTokenGenerator.generateToken(userId, role);

            // Set the token as a cookie
            cookieService.createJwtCookie(newToken, response);

            // Log the successful token renewal
            loggingService.logAction(userId.intValue(), userId.toString(), "JWT token renewed successfully");

            // Return success response
            result.put("success", true);
            result.put("message", "Token renewed successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error renewing JWT token: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while renewing the token");
        }

        return result;
    }
}
