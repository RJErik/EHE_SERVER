package ehe_server.service.auth;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.User;
import ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.EmailAlreadyRegisteredException;
import ehe_server.exception.custom.EmailSendFailureException;
import ehe_server.repository.UserRepository;
import ehe_server.repository.VerificationTokenRepository;
import ehe_server.properties.VerificationTokenProperties;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.auth.RegistrationServiceInterface;
import ehe_server.service.intf.email.EmailSenderServiceInterface;
import ehe_server.service.intf.token.TokenHashServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(noRollbackFor = EmailSendFailureException.class)
public class RegistrationService implements RegistrationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSenderServiceInterface emailSenderService;
    private final UserContextService userContextService;
    private final VerificationTokenProperties verificationTokenProperties;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashServiceInterface tokenHashService;

    public RegistrationService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailSenderServiceInterface emailSenderService,
            UserContextService userContextService,
            VerificationTokenProperties verificationTokenProperties,
            PasswordEncoder passwordEncoder,
            TokenHashServiceInterface tokenHashService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailSenderService = emailSenderService;
        this.userContextService = userContextService;
        this.verificationTokenProperties = verificationTokenProperties;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(messageKey = "log.message.auth.registration")
    @Override
    public void registerUser(String username, String email, String password) {

        // Database integrity checks
        if (userRepository.findByEmail(email).isPresent()) {
            // We consciously reveal this to help the user log in instead
            throw new EmailAlreadyRegisteredException(email).withActionLink("log in.", "login");
        }

        // Entity construction
        User newUser = new User();
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setAccountStatus(User.AccountStatus.NONVERIFIED);

        User savedUser = userRepository.save(newUser);

        // Audit context setup
        userContextService.setUser(String.valueOf(savedUser.getUserId()), "USER");

        // Token generation
        String plainToken = UUID.randomUUID().toString();  // Plain token to send via email
        String hashedToken = tokenHashService.hashToken(plainToken);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());

        VerificationToken verificationToken = new VerificationToken(
                savedUser, hashedToken, VerificationToken.TokenType.REGISTRATION, expiryDate);
        verificationTokenRepository.save(verificationToken);

        // Email notification
        try {
            emailSenderService.sendRegistrationVerificationEmail(savedUser, plainToken, email);
        } catch (EmailSendFailureException e) {
            // Because of 'noRollbackFor', the User and Token are SAVED in the DB.
            // The Frontend catches this 500/400 error and shows "Resend Email" button.
            // The 'Resend' endpoint will then find this 'NONVERIFIED' user and try again.
            throw new EmailSendFailureException(email, e.getMessage()).withResendButton();
        }
    }
}