package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.token.TokenHashServiceInterface;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
@Transactional
public class JwtRefreshTokenService implements JwtRefreshTokenServiceInterface {

    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final LoggingServiceInterface loggingService;
    private final TokenHashServiceInterface tokenHashService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> nextCleanupTask;
    private LocalDateTime nextScheduledCleanup;
    private final Object scheduleLock = new Object();

    public JwtRefreshTokenService(JwtRefreshTokenRepository jwtRefreshTokenRepository,
                                  LoggingServiceInterface loggingService,
                                  TokenHashServiceInterface tokenHashService) {
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.loggingService = loggingService;
        this.tokenHashService = tokenHashService;

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("jwt-cleanup-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void initialize() {
        loggingService.logAction("Initializing JWT refresh token cleanup service");
        performImmediateCleanup();
        scheduleNextCleanup();
    }

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
        saveRefreshToken(user, tokenHash, expirationTime,
                LocalDateTime.now().plusSeconds(maxExpirationTime / 1000));
    }

    @Transactional
    public void saveRefreshToken(User user, String tokenHash, long expirationTime, LocalDateTime specificMaxExpiry) {
        JwtRefreshToken refreshToken = new JwtRefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJwtRefreshTokenHash(tokenHash);
        refreshToken.setJwtRefreshTokenExpiryDate(
                LocalDateTime.now().plusSeconds(expirationTime / 1000)
        );
        refreshToken.setJwtRefreshTokenMaxExpiryDate(specificMaxExpiry);

        JwtRefreshToken savedToken = jwtRefreshTokenRepository.save(refreshToken);
        loggingService.logAction("JWT refresh token saved for user: " + user.getUserId());

        synchronized (scheduleLock) {
            if (nextScheduledCleanup == null ||
                    savedToken.getJwtRefreshTokenExpiryDate().isBefore(nextScheduledCleanup)) {
                loggingService.logAction("Rescheduling cleanup - new token expires sooner");
                scheduleNextCleanup();
            }
        }
    }

    /**
     * Removes token by its raw value and returns the MaxExpiryDate for session anchoring.
     * Returns NULL if token is not found (implies theft/reuse).
     */
    @Override
    @Transactional
    public LocalDateTime removeRefreshTokenByToken(String token) {
        try {
            // Hash the token and look it up directly
            String hash = tokenHashService.hashToken(token);
            Optional<JwtRefreshToken> storedTokenOpt = jwtRefreshTokenRepository.findByJwtRefreshTokenHash(hash);

            if (storedTokenOpt.isPresent()) {
                JwtRefreshToken storedToken = storedTokenOpt.get();
                LocalDateTime retainedMaxExpiry = storedToken.getJwtRefreshTokenMaxExpiryDate();

                deleteTokenAndReschedule(
                        storedToken.getJwtRefreshTokenId(),
                        storedToken.getJwtRefreshTokenExpiryDate()
                );

                return retainedMaxExpiry;
            }

            // Token not found in DB - possible theft/reuse
            loggingService.logAction("Refresh token not found in database");
        } catch (Exception e) {
            loggingService.logError("Error removing refresh token: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    @Transactional
    public void removeAllUserTokens(Integer userId) {
        List<JwtRefreshToken> userTokens = jwtRefreshTokenRepository.findByUser_UserId(userId);

        if (!userTokens.isEmpty()) {
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

            if (needsReschedule) {
                triggerSchedulerUpdate();
            }
        }
    }

    private void deleteTokenAndReschedule(Integer tokenId, LocalDateTime expiryDate) {
        try {
            jwtRefreshTokenRepository.deleteById(tokenId);
            jwtRefreshTokenRepository.flush();

            loggingService.logAction("JWT refresh token removed: " + tokenId);

            final LocalDateTime capturedExpiryDate = expiryDate;
            final LocalDateTime capturedNextScheduledCleanup = nextScheduledCleanup;

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    synchronized (scheduleLock) {
                        if (capturedExpiryDate.equals(capturedNextScheduledCleanup)) {
                            loggingService.logAction("Rescheduling cleanup - removed next expiring token");
                            triggerSchedulerUpdate();
                        }
                    }
                }
            });

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                 org.springframework.dao.EmptyResultDataAccessException e) {
            loggingService.logAction("JWT refresh token " + tokenId + " was already deleted (concurrent request)");
        }
    }

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

    private void triggerSchedulerUpdate() {
        scheduleNextCleanup();
    }

    private void scheduleNextCleanup() {
        synchronized (scheduleLock) {
            if (nextCleanupTask != null && !nextCleanupTask.isDone()) {
                nextCleanupTask.cancel(false);
                nextCleanupTask = null;
            }

            LocalDateTime now = LocalDateTime.now();
            Optional<JwtRefreshToken> nextExpiringToken =
                    jwtRefreshTokenRepository.findFirstByJwtRefreshTokenExpiryDateAfterOrderByJwtRefreshTokenExpiryDateAsc(now);

            if (nextExpiringToken.isPresent()) {
                nextScheduledCleanup = nextExpiringToken.get().getJwtRefreshTokenExpiryDate();

                long delayMillis = Duration.between(now, nextScheduledCleanup).toMillis();
                delayMillis += 1000;

                if (delayMillis < 0) {
                    delayMillis = 0;
                }

                Instant scheduledTime = Instant.now().plusMillis(delayMillis);
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

    private void performScheduledCleanup() {
        loggingService.logAction("Executing scheduled JWT token cleanup");
        performImmediateCleanup();
        scheduleNextCleanup();
    }
}