package com.example.ehe_server.service.auth;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.LogInServiceInterface;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class LogInService implements LogInServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private final UserContextService userContextService;

    public LogInService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        JwtTokenGeneratorInterface jwtTokenGenerator,
                        CookieServiceInterface cookieService,
                        LoggingServiceInterface loggingService,
                        UserContextService userContextService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public void authenticateUser(LoginRequest request, HttpServletResponse response) {
        // Validate both fields are provided
        if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
                (request.getPassword() == null || request.getPassword().trim().isEmpty())) {
            throw new MissingLoginCredentialsException();
        }

        // Validate email is provided
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new MissingEmailException();
        }

        // Validate password is provided
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new MissingPasswordException();
        }

        // Validate email format
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new InvalidEmailFormatException(request.getEmail());
        }

        // Find user by email hash
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            throw new InvalidEmailFormatException(request.getEmail()).withActionLink("registering.", "register");
        }

        User user = userOpt.get();

        // Check account status

        if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
            throw new NonVerifiedAccountException(request.getEmail(), user.getAccountStatus().toString()).withResendButton();
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new InactiveAccountException(request.getEmail(), user.getAccountStatus().toString());
        }

        // Verify password
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(request.getEmail());
        }

        // Check if user is an admin
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }

        // Update audit context with authenticated user and roles
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Generate JWT token with user ID and roles
        String jwtAccessToken = jwtTokenGenerator.generateAccessToken(Long.valueOf(user.getUserId()), role);
        String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(Long.valueOf(user.getUserId()), role);

        // Create JWT cookie
        cookieService.addJwtAccessCookie(jwtAccessToken, response);
        cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

        // Log successful login
        loggingService.logAction("Login successful");
    }
}
