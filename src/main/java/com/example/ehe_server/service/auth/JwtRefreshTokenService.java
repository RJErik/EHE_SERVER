package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
public class JwtRefreshTokenService implements JwtRefreshTokenServiceInterface {

    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final LoggingServiceInterface loggingService;
    private final TaskScheduler taskScheduler;

    // Track the next scheduled cleanup
    private ScheduledFuture<?> nextCleanupTask;
    private LocalDateTime nextScheduledCleanup;
    private final Object scheduleLock = new Object();

    public JwtRefreshTokenService(JwtRefreshTokenRepository jwtRefreshTokenRepository,
                                  LoggingServiceInterface loggingService) {
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.loggingService = loggingService;

        // Create a task scheduler for dynamic scheduling
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("jwt-cleanup-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    /**
     * Initialize cleanup on application startup
     */
    @PostConstruct
    public void initialize() {
        loggingService.logAction("Initializing JWT refresh token cleanup service");
        performImmediateCleanup();
        scheduleNextCleanup();
    }

    /**
     * Cancel scheduled tasks on shutdown
     */
    @PreDestroy
    public void shutdown() {
        synchronized (scheduleLock) {
            if (nextCleanupTask != null) {
                nextCleanupTask.cancel(false);
                loggingService.logAction("JWT cleanup task cancelled");
            }
        }
    }

    @Override
    @Transactional
    public void saveRefreshToken(User user, String tokenHash, long expirationTime, long maxExpirationTime) {
        JwtRefreshToken refreshToken = new JwtRefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJwtRefreshTokenHash(tokenHash);
        refreshToken.setJwtRefreshTokenExpiryDate(
                LocalDateTime.now().plusSeconds(expirationTime / 1000)
        );
        refreshToken.setJwtRefreshTokenMaxExpiryDate(
                LocalDateTime.now().plusSeconds(maxExpirationTime / 1000)
        );

        JwtRefreshToken savedToken = jwtRefreshTokenRepository.save(refreshToken);

        loggingService.logAction("JWT refresh token saved for user: " + user.getUserId());

        // Check if this token expires sooner than the currently scheduled cleanup
        synchronized (scheduleLock) {
            if (nextScheduledCleanup == null ||
                    savedToken.getJwtRefreshTokenExpiryDate().isBefore(nextScheduledCleanup)) {
                loggingService.logAction("Rescheduling cleanup - new token expires sooner");
                scheduleNextCleanup();
            }
        }
    }

    @Override
    @Transactional
    public void removeRefreshToken(Integer tokenId) {
        Optional<JwtRefreshToken> tokenOpt = jwtRefreshTokenRepository.findById(tokenId);

        if (tokenOpt.isPresent()) {
            LocalDateTime expiryDate = tokenOpt.get().getJwtRefreshTokenExpiryDate();
            jwtRefreshTokenRepository.deleteById(tokenId);

            loggingService.logAction("JWT refresh token removed: " + tokenId);

            // If we removed the token that was scheduled for next cleanup, reschedule
            synchronized (scheduleLock) {
                if (nextScheduledCleanup != null && expiryDate.equals(nextScheduledCleanup)) {
                    loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                    scheduleNextCleanup();
                }
            }
        }
    }

    @Override
    @Transactional
    public void removeRefreshTokenByHash(String tokenHash) {
        Optional<JwtRefreshToken> tokenOpt = jwtRefreshTokenRepository.findByJwtRefreshTokenHash(tokenHash);
        tokenOpt.ifPresent(token -> removeRefreshToken(token.getJwtRefreshTokenId()));
    }

    @Override
    @Transactional
    public void removeAllUserTokens(Integer userId) {
        List<JwtRefreshToken> userTokens = jwtRefreshTokenRepository.findByUser_UserId(userId);

        if (!userTokens.isEmpty()) {
            // Check if any of the tokens being removed is the next scheduled one
            boolean needsReschedule = false;
            synchronized (scheduleLock) {
                if (nextScheduledCleanup != null) {
                    for (JwtRefreshToken token : userTokens) {
                        if (token.getJwtRefreshTokenExpiryDate().equals(nextScheduledCleanup)) {
                            needsReschedule = true;
                            break;
                        }
                    }
                }
            }

            jwtRefreshTokenRepository.deleteAll(userTokens);
            loggingService.logAction("Removed all JWT refresh tokens for user: " + userId);

            if (needsReschedule) {
                loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                scheduleNextCleanup();
            }
        }
    }

    /**
     * Perform immediate cleanup of expired tokens
     */
    @Transactional
    private void performImmediateCleanup() {
        LocalDateTime now = LocalDateTime.now();
        List<JwtRefreshToken> expiredTokens =
                jwtRefreshTokenRepository.findByJwtRefreshTokenExpiryDateBefore(now);

        if (!expiredTokens.isEmpty()) {
            jwtRefreshTokenRepository.deleteAll(expiredTokens);
            loggingService.logAction("Cleaned up " + expiredTokens.size() + " expired JWT refresh tokens");
        } else {
            loggingService.logAction("No expired JWT refresh tokens found");
        }
    }

    /**
     * Schedule the next cleanup based on the earliest expiring token
     */
    private void scheduleNextCleanup() {
        synchronized (scheduleLock) {
            // Cancel existing scheduled task
            if (nextCleanupTask != null && !nextCleanupTask.isDone()) {
                nextCleanupTask.cancel(false);
                nextCleanupTask = null;
            }

            // Find the earliest expiring token that hasn't expired yet
            LocalDateTime now = LocalDateTime.now();
            Optional<JwtRefreshToken> nextExpiringToken =
                    jwtRefreshTokenRepository.findFirstByJwtRefreshTokenExpiryDateAfterOrderByJwtRefreshTokenExpiryDateAsc(now);

            if (nextExpiringToken.isPresent()) {
                nextScheduledCleanup = nextExpiringToken.get().getJwtRefreshTokenExpiryDate();

                // Calculate delay until expiration
                long delayMillis = Duration.between(now, nextScheduledCleanup).toMillis();

                // Add a small buffer (1 second) to ensure the token is actually expired
                delayMillis += 1000;

                // Ensure delay is positive
                if (delayMillis < 0) {
                    delayMillis = 0;
                }

                // Schedule the cleanup
                Date scheduledTime = new Date(System.currentTimeMillis() + delayMillis);
                nextCleanupTask = taskScheduler.schedule(
                        this::performScheduledCleanup,
                        scheduledTime
                );

                loggingService.logAction(
                        String.format("Next JWT cleanup scheduled for: %s (in %d seconds)",
                                nextScheduledCleanup, delayMillis / 1000)
                );
            } else {
                nextScheduledCleanup = null;
                loggingService.logAction("No JWT tokens to schedule cleanup for");
            }
        }
    }

    /**
     * Executed when a scheduled cleanup runs
     */
    @Transactional
    private void performScheduledCleanup() {
        loggingService.logAction("Executing scheduled JWT token cleanup");
        performImmediateCleanup();

        // Schedule the next cleanup
        scheduleNextCleanup();
    }
}