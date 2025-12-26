package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.LogInServiceInterface;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class LogInService implements LogInServiceInterface {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final JwtProperties jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final UserContextService userContextService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    public LogInService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        JwtRefreshTokenServiceInterface jwtRefreshTokenService,
                        JwtTokenGeneratorInterface jwtTokenGenerator,
                        CookieServiceInterface cookieService,
                        UserContextService userContextService,
                        JwtProperties jwtConfig,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.userContextService = userContextService;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(
            messageKey = "log.message.auth.login"
    )
    @Override
    public void authenticateUser(String email, String password, HttpServletResponse response) {

        // Input validation checks
        if ((email == null || email.trim().isEmpty()) && (password == null || password.trim().isEmpty())) {
            throw new MissingLoginCredentialsException();
        }

        if (email == null || email.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (password == null || password.trim().isEmpty()) {
            throw new MissingPasswordException();
        }

        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordFormatException();
        }

        // Database integrity checks
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserEmailNotFoundWithPasswordMessageException(email);
        }

        User user = userOpt.get();

        // Credential verification
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException(email);
        }

        // Role determination and context setup
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";

        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Account status verification
        if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
            throw new NonVerifiedAccountException(email, user.getAccountStatus().toString()).withResendButton();
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new InactiveAccountException(email, user.getAccountStatus().toString());
        }

        // Token generation and execution
        String jwtAccessToken = jwtTokenGenerator.generateAccessToken(user.getUserId(), role);
        String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(user.getUserId(), role);

        cookieService.addJwtAccessCookie(jwtAccessToken, response);
        cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

        String refreshTokenHash = passwordEncoder.encode(jwtRefreshToken);
        jwtRefreshTokenService.saveRefreshToken(
                user,
                refreshTokenHash,
                jwtConfig.getJwtRefreshExpirationTime(),
                jwtConfig.getJwtRefreshTokenMaxExpireTime()
        );
    }
}