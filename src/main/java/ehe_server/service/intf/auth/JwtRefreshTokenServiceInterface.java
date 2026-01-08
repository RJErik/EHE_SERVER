package ehe_server.service.intf.auth;

import ehe_server.entity.User;

import java.time.LocalDateTime;

public interface JwtRefreshTokenServiceInterface {

    /**
     * Save a refresh token with standard expiration times (calculates max expiry relative to now).
     *
     * @param user              The user associated with the token
     * @param tokenHash         The hashed refresh token
     * @param expirationTime    Expiration time in milliseconds (Sliding Window)
     * @param maxExpirationTime Maximum expiration time in milliseconds (Absolute Timeout)
     */
    void saveRefreshToken(User user, String tokenHash, long expirationTime, long maxExpirationTime);

    /**
     * Save a refresh token with a specific, fixed Max Expiry date.
     * This is used during token rotation to preserve the "Anchor" date of the session chain.
     *
     * @param user              The user associated with the token
     * @param tokenHash         The hashed refresh token
     * @param expirationTime    Expiration time in milliseconds (Sliding Window)
     * @param specificMaxExpiry The specific LocalDateTime when the session chain must end
     */
    void saveRefreshToken(User user, String tokenHash, long expirationTime, LocalDateTime specificMaxExpiry);

    /**
     * Remove a refresh token by the raw token string.
     * Used during rotation or logout.
     *
     * @param token The raw JWT refresh token string
     * @return The max expiry date of the removed token (for anchoring), or null if not found
     */
    LocalDateTime removeRefreshTokenByToken(String token);

    /**
     * Remove all refresh tokens for a specific user.
     * Used for "Nuclear Option" security events or full logout.
     *
     * @param userId The user ID
     */
    void removeAllUserTokens(Integer userId);
}