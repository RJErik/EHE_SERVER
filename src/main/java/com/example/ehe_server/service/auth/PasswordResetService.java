package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.InvalidPasswordFormatException;
import com.example.ehe_server.exception.custom.MissingPasswordException;
import com.example.ehe_server.exception.custom.PasswordResetTokenNotFoundException;
import com.example.ehe_server.exception.custom.UserNotFoundForTokenException;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;

import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class PasswordResetService implements PasswordResetServiceInterface {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final LoggingServiceInterface loggingService;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;

    public PasswordResetService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            LoggingServiceInterface loggingService,
            AdminRepository adminRepository,
            UserContextService userContextService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {

        // Validate token first
        passwordResetTokenValidationService.validatePasswordResetToken(token);

        // Validate password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new MissingPasswordException();
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new InvalidPasswordFormatException();
        }

        // Get token and user
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        // This should not happen as we validated the token, but check anyway
        if (tokenOpt.isEmpty()) {
            throw new PasswordResetTokenNotFoundException(token);
        }

        VerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();

        // Check if user is null
        if (user == null) {
            throw new UserNotFoundForTokenException(token).withActionLink("registering.", "register");
        }

        // Update audit context
        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Set audit context
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(userIdStr, role);

        // Update password
        String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(passwordHash);

        // If account was NONVERIFIED, activate it
        if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            loggingService.logAction("User account activated during password reset");
        }

        // Mark token as used
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        // Save changes
        userRepository.save(user);
        verificationTokenRepository.save(verificationToken);

        loggingService.logAction("Password reset successful");
    }
}
