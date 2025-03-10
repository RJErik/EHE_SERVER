package com.example.ehe_server.service.auth;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.entities.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.security.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.auth.AuthenticationServiceInterface;
import com.example.ehe_server.service.intf.auth.HashingServiceInterface;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthenticationService implements AuthenticationServiceInterface {

    private final UserRepository userRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final HashingServiceInterface hashingService;
    private final CookieServiceInterface cookieService;

    public AuthenticationService(UserRepository userRepository,
                                 JwtTokenGeneratorInterface jwtTokenGenerator,
                                 HashingServiceInterface hashingService,
                                 CookieServiceInterface cookieService) {
        this.userRepository = userRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.hashingService = hashingService;
        this.cookieService = cookieService;
    }

    @Override
    public Map<String, Object> authenticateUser(LoginRequest request, HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Hash the email for lookup
            String emailHash = hashingService.hashEmail(request.getEmail());

            // Find user by email hash
            Optional<User> userOpt = userRepository.findByEmailHash(emailHash);

            if (userOpt.isEmpty()) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password");
                return responseBody;
            }

            User user = userOpt.get();

            // Check account status
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                responseBody.put("success", false);
                responseBody.put("message", "Your account is not active");
                return responseBody;
            }

            // Verify password
            if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password");
                return responseBody;
            }

            // Generate JWT token
            String jwtToken = jwtTokenGenerator.generateToken(Long.valueOf(user.getUserId()));

            // Create JWT cookie
            cookieService.createJwtCookie(jwtToken, response);

            // Return success response
            responseBody.put("success", true);
            responseBody.put("message", "Login successful");
            responseBody.put("userName", user.getUserName());

            return responseBody;

        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("message", "An error occurred during login");
            return responseBody;
        }
    }

    @Override
    public void logoutUser(HttpServletResponse response) {
        cookieService.clearJwtCookie(response);
    }
}
