package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.user.JwtTokenRenewalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public JwtTokenRenewalService(
            AdminRepository adminRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> renewToken(Long userId, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();

            // Get the user's role
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Create roles list based on user status
            String role = "USER"; // All authenticated users have USER role

            if (isAdmin) {
                role = "ADMIN"; // Add ADMIN role if user is in Admin table
            }

            // Update audit context with authenticated user and roles
            userContextService.setUser(String.valueOf(user.getUserId()), role);

            // Generate a new token
            String newToken = jwtTokenGenerator.generateToken(userId, role);

            // Set the token as a cookie
            cookieService.createJwtCookie(newToken, response);

            // Log the successful token renewal
            loggingService.logAction("JWT token renewed successfully");

            // Return success response
            result.put("success", true);
            result.put("message", "Token renewed successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError("Error renewing JWT token: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while renewing the token");
        }

        return result;
    }
}
