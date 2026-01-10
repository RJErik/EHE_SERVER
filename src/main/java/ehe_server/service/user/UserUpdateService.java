package ehe_server.service.user;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.UserResponse;
import ehe_server.entity.User;
import ehe_server.entity.VerificationToken;
import ehe_server.exception.custom.AdminModificationForbiddenException;
import ehe_server.exception.custom.EmailAlreadyRegisteredException;
import ehe_server.exception.custom.UserNotFoundException;
import ehe_server.repository.AdminRepository;
import ehe_server.repository.UserRepository;
import ehe_server.repository.VerificationTokenRepository;
import ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import ehe_server.service.intf.user.UserUpdateServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserUpdateService implements UserUpdateServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;

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
    public UserResponse updateUserInfo(Integer userId, String username, String email, String password, User.AccountStatus accountStatus) {

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (adminRepository.existsByAdminId(userId)) {
            throw new AdminModificationForbiddenException(userId);
        }

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

        if (accountStatus != null) {
            user.setAccountStatus(accountStatus);
            shouldRevokeTokens = true;
        }

        if (shouldRevokeTokens) {
            jwtRefreshTokenService.removeAllUserTokens(userId);
        }

        return new UserResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getAccountStatus(),
                user.getRegistrationDate()
        );
    }
}