package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.EmailChangeRequestRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.EmailChangeVerificationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class EmailChangeVerificationService implements EmailChangeVerificationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;

    public EmailChangeVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            UserRepository userRepository,
            UserContextService userContextService,
            AdminRepository adminRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
    }


    @LogMessage(
            messageKey = "log.message.auth.emailChangeVerification",
            params = {"#token"}
    )
    @Override
    public void validateEmailChange(String token) {

        // Find the token
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new InvalidVerificationTokenException(token);
        }

        VerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();
        String userIdStr = String.valueOf(user.getUserId());

        // Set audit context
        // Check if user is an admin
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(userIdStr, role);

        // Check token status
        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }

        // Check token type
        if (verificationToken.getTokenType() != VerificationToken.TokenType.EMAIL_CHANGE) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.EMAIL_CHANGE, verificationToken.getTokenType());
        }

        // Check if token is expired
        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            verificationTokenRepository.save(verificationToken);

            throw new ExpiredVerificationTokenException(token);
        }

        // Check user status
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new InactiveAccountException(userIdStr, user.getAccountStatus().toString());
        }

        // Find email change request
        Optional<EmailChangeRequest> requestOpt = emailChangeRequestRepository.findByVerificationToken(verificationToken);

        if (requestOpt.isEmpty()) {
            throw new EmailChangeRequestNotFoundException(token);
        }

        EmailChangeRequest emailChangeRequest = requestOpt.get();
        String newEmail = emailChangeRequest.getNewEmail();

        // Check if the new email is already in use by another user
        Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
        if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getUserId().equals(user.getUserId())) {
            throw new EmailAlreadyInUseException(newEmail);
        }

        // Update user's email
        user.setEmail(newEmail);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);
        verificationTokenRepository.save(verificationToken);
    }
}
