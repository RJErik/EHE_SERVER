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
import com.example.ehe_server.service.intf.token.TokenHashServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService implements PasswordResetServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserContextServiceInterface userContextService;
    private final TokenHashServiceInterface tokenHashService;

    public PasswordResetService(
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService,
            PasswordEncoder passwordEncoder,
            UserContextServiceInterface userContextService,
            TokenHashServiceInterface tokenHashService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(
            messageKey = "log.message.auth.passwordReset",
            params = {"#token"}
    )
    @Override
    public void resetPassword(String token, String newPassword) {

        // External Validation (Throws exception if invalid, sets UserContext)
        passwordResetTokenValidationService.validatePasswordResetToken(token);

        // Data Retrieval
        String hashedToken = tokenHashService.hashToken(token);

        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidVerificationTokenException(token));


        User user = verificationToken.getUser();

        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(userIdStr, role);

        // Updates
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
        }

        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        // Authentication update
        jwtRefreshTokenService.removeAllUserTokens(user.getUserId());
    }
}