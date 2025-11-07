package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.UserUpdateResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserUpdateServiceInterface;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserUpdateService implements UserUpdateServiceInterface {

    private final UserRepository userRepository;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final LoggingServiceInterface loggingService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    public UserUpdateService(UserRepository userRepository,
                             LoggingServiceInterface loggingService,
                             JwtRefreshTokenServiceInterface jwtRefreshTokenService) {
        this.userRepository = userRepository;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.loggingService = loggingService;
    }
    @Override
    public UserUpdateResponse updateUserInfo(Integer userId, String username, String email, String password, String accountStatus) {

        User.AccountStatus userStatus = null;
        if (accountStatus != null && !accountStatus.trim().isEmpty()) {
            try {
                userStatus = User.AccountStatus.valueOf(accountStatus);
            } catch (IllegalArgumentException e) {
                loggingService.logAction("Invalid account type: " + accountStatus);
                return null;
            }
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId.longValue());
        }


        User user = userOpt.get();

        if (username != null && !username.trim().isEmpty()) {
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                throw new InvalidUsernameFormatException(username);
            } else {
                user.setUserName(username);
            }
        }

        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new InvalidEmailFormatException(email);
            } else if (userRepository.findByEmail(email).isPresent()) {
                throw new EmailAlreadyRegisteredException(email);
            } else {
                user.setEmail(email);
            }
        }

        if (password != null && !password.trim().isEmpty()) {
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                throw new InvalidPasswordFormatException();
            } else {
                user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            }
        }

        if (userStatus != null) {
            user.setAccountStatus(userStatus);
        }

        jwtRefreshTokenService.removeAllUserTokens(userId);

        loggingService.logAction("Updated user with id: " + userId);

        return new UserUpdateResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getAccountStatus().toString(),
                user.getRegistrationDate().format(DATE_FORMATTER)
        );
    }
}
