package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class PasswordResetService implements PasswordResetServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserContextServiceInterface userContextService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$");

    // Note: AdminRepository and UserContextService removed (handled by validator)
    public PasswordResetService(
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService,
            PasswordEncoder passwordEncoder,
            UserContextServiceInterface userContextService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @LogMessage(
            messageKey = "log.message.auth.passwordReset",
            params = {"#token"}
    )
    @Override
    public void resetPassword(String token, String newPassword) {

        // External Validation (Throws exception if invalid, sets UserContext)
        passwordResetTokenValidationService.validatePasswordResetToken(token);

        // Input Validation (Password specific)
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new MissingPasswordException();
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new InvalidPasswordFormatException();
        }

        // Data Retrieval
        // We fetch blindly because the validator guaranteed existence.
        // using orElseThrow is just a sanity check for the compiler/runtime safety.
        VerificationToken verificationToken = findTokenByPlainValue(
                token
        );

        if (verificationToken == null) {
            throw new PasswordResetTokenNotFoundException(token);
        }


        User user = verificationToken.getUser();

        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(userIdStr, role);

        // 4. State Updates
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
        }

        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        // Authentication  update
        jwtRefreshTokenService.removeAllUserTokens(user.getUserId());
    }

    private VerificationToken findTokenByPlainValue(String plainToken) {
        List<VerificationToken> activeTokens = verificationTokenRepository.findByTokenTypeAndStatus(
                VerificationToken.TokenType.PASSWORD_RESET,
                VerificationToken.TokenStatus.ACTIVE
        );

        return activeTokens.stream()
                .filter(token -> passwordEncoder.matches(plainToken, token.getTokenHash()))
                .findFirst()
                .orElse(null);
    }

}