package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.UserResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.user.UserUpdateServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserUpdateService implements UserUpdateServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,100}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    public UserUpdateService(UserRepository userRepository,
                             AdminRepository adminRepository,
                             VerificationTokenRepository verificationTokenRepository,
                             JwtRefreshTokenServiceInterface jwtRefreshTokenService,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.update",
            params = {
                    "#userId",
                    "#username",
                    "#email",
                    "#accountStatus",
                    "#result.registrationDate"
            }
    )
    @Override
    public UserResponse updateUserInfo(Integer userId, String username, String email, String password, String accountStatus) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Parsing and Format Validation
        User.AccountStatus userStatus = null;
        if (accountStatus != null && !accountStatus.trim().isEmpty()) {
            try {
                userStatus = User.AccountStatus.valueOf(accountStatus);
            } catch (IllegalArgumentException e) {
                throw new InvalidAccountStatusException(accountStatus);
            }
        }

        if (username != null && !username.trim().isEmpty()) {
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                throw new InvalidUsernameFormatException(username);
            }
        }

        if (email != null && !email.trim().isEmpty()) {
            if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new InvalidEmailFormatException(email);
            }
        }

        if (password != null && !password.trim().isEmpty()) {
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                throw new InvalidPasswordFormatException();
            }
        }

        // Database integrity checks
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        if (adminRepository.existsByAdminId(userId)) {
            throw new AdminModificationForbiddenException(userId);
        }

        User user = userOpt.get();
        boolean shouldRevokeTokens = false;

        // Contextual validation
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
                throw new EmailAlreadyRegisteredException(email);
            }
        }

        // Execution and updates
        if (username != null && !username.trim().isEmpty()) {
            user.setUserName(username);
        }

        if (email != null && !email.trim().isEmpty()) {
            List<VerificationToken> tokens = verificationTokenRepository
                    .findByUser_UserIdAndTokenTypeAndStatus(
                            userId,
                            VerificationToken.TokenType.EMAIL_CHANGE,
                            VerificationToken.TokenStatus.ACTIVE
                    );

            tokens.forEach(t -> t.setStatus(VerificationToken.TokenStatus.INVALIDATED));

            user.setEmail(email);
            shouldRevokeTokens = true;
        }

        if (password != null && !password.trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
            shouldRevokeTokens = true;
        }

        if (userStatus != null) {
            user.setAccountStatus(userStatus);
            shouldRevokeTokens = true;
        }

        if (shouldRevokeTokens) {
            jwtRefreshTokenService.removeAllUserTokens(userId);
        }

        return new UserResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getAccountStatus().toString(),
                user.getRegistrationDate().format(DATE_FORMATTER)
        );
    }
}