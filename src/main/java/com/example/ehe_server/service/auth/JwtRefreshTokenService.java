package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPublicKey;
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
    private final RSAPublicKey publicKey;

    // Track the next scheduled cleanup
    private ScheduledFuture<?> nextCleanupTask;
    private LocalDateTime nextScheduledCleanup;
    private final Object scheduleLock = new Object();

    public JwtRefreshTokenService(JwtRefreshTokenRepository jwtRefreshTokenRepository,
                                  LoggingServiceInterface loggingService,
                                  RSAPublicKey publicKey) {
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.loggingService = loggingService;
        this.publicKey = publicKey;

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
    public void removeRefreshTokenById(Integer tokenId) {
        try {
            Optional<JwtRefreshToken> tokenOpt = jwtRefreshTokenRepository.findById(tokenId);

            if (tokenOpt.isPresent()) {
                LocalDateTime expiryDate = tokenOpt.get().getJwtRefreshTokenExpiryDate();

                try {
                    jwtRefreshTokenRepository.deleteById(tokenId);

                    // Flush to ensure delete completes
                    jwtRefreshTokenRepository.flush();

                    loggingService.logAction("JWT refresh token removed: " + tokenId);

                    // FIXED: Schedule cleanup AFTER transaction commits using a separate thread
                    // This prevents deadlock by not querying the same table we just deleted from
                    final LocalDateTime capturedExpiryDate = expiryDate;
                    final LocalDateTime capturedNextScheduledCleanup = nextScheduledCleanup;

                    // Run in separate thread after a short delay to ensure transaction commits
                    taskScheduler.schedule(() -> {
                        synchronized (scheduleLock) {
                            if (capturedNextScheduledCleanup != null &&
                                    capturedExpiryDate.equals(capturedNextScheduledCleanup)) {
                                loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                                scheduleNextCleanupInNewTransaction();
                            }
                        }
                    }, new Date(System.currentTimeMillis() + 100)); // 100ms delay

                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                         org.springframework.dao.EmptyResultDataAccessException e) {
                    // Token was already deleted by another concurrent request - this is fine
                    loggingService.logAction("JWT refresh token " + tokenId + " was already deleted (concurrent request)");
                }
            } else {
                loggingService.logAction("JWT refresh token " + tokenId + " not found (may have been already deleted)");
            }
        } catch (Exception e) {
            loggingService.logError("Error removing refresh token by ID: " + e.getMessage(), e);
            // Don't rethrow - token removal failure shouldn't block the operation
        }
    }

    @Override
    @Transactional
    public void removeRefreshTokenByToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extract user_id from the token claims
            Long userId = claims.get("user_id", Long.class);

            // Get only this user's refresh tokens from the database
            List<JwtRefreshToken> userRefreshTokens = jwtRefreshTokenRepository.findByUser_UserId(userId.intValue());

            // Check if the provided token matches any of this user's stored hashes
            for (JwtRefreshToken storedToken : userRefreshTokens) {
                if (BCrypt.checkpw(token, storedToken.getJwtRefreshTokenHash())) {
                    // Delete directly without calling removeRefreshTokenById to avoid nested transaction issues
                    LocalDateTime expiryDate = storedToken.getJwtRefreshTokenExpiryDate();
                    Integer tokenId = storedToken.getJwtRefreshTokenId();

                    try {
                        jwtRefreshTokenRepository.deleteById(tokenId);
                        jwtRefreshTokenRepository.flush();

                        loggingService.logAction("JWT refresh token removed: " + tokenId);

                        // Schedule cleanup in separate thread after transaction commits
                        final LocalDateTime capturedExpiryDate = expiryDate;
                        final LocalDateTime capturedNextScheduledCleanup = nextScheduledCleanup;

                        taskScheduler.schedule(() -> {
                            synchronized (scheduleLock) {
                                if (capturedNextScheduledCleanup != null &&
                                        capturedExpiryDate.equals(capturedNextScheduledCleanup)) {
                                    loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                                    scheduleNextCleanupInNewTransaction();
                                }
                            }
                        }, new Date(System.currentTimeMillis() + 100)); // 100ms delay

                    } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                             org.springframework.dao.EmptyResultDataAccessException e) {
                        // Token was already deleted by another concurrent request - this is fine
                        loggingService.logAction("JWT refresh token " + tokenId + " was already deleted (concurrent request)");
                    }

                    break; // Only remove one token
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error removing refresh token: " + e.getMessage(), e);
            // Don't rethrow - token removal failure shouldn't block token renewal
        }
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
            jwtRefreshTokenRepository.flush();
            loggingService.logAction("Removed all JWT refresh tokens for user: " + userId);

            // Schedule cleanup in separate thread after transaction commits
            if (needsReschedule) {
                taskScheduler.schedule(() -> {
                    loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                    scheduleNextCleanupInNewTransaction();
                }, new Date(System.currentTimeMillis() + 100)); // 100ms delay
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
     * This method runs in a NEW transaction to avoid deadlocks
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void scheduleNextCleanupInNewTransaction() {
        scheduleNextCleanup();
    }

    /**
     * Schedule the next cleanup based on the earliest expiring token
     * WARNING: This method queries the database - should not be called within
     * the same transaction that's modifying the jwt_refresh_token table
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