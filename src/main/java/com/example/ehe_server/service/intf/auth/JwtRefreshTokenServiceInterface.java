package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.entity.User;

public interface JwtRefreshTokenServiceInterface {

    /**
     * Save a new JWT refresh token
     * @param user The user associated with the token
     * @param tokenHash The hashed refresh token
     * @param expirationTime Expiration time in milliseconds
     * @param maxExpirationTime Maximum expiration time in milliseconds
     */
    void saveRefreshToken(User user, String tokenHash, long expirationTime, long maxExpirationTime);

    /**
     * Remove a refresh token by ID
     * @param tokenId The ID of the token to remove
     */
    void removeRefreshTokenById(Integer tokenId);

    /**
     * Remove a refresh token by token hash
     * @param token The token to remove
     */
    void removeRefreshTokenByToken(String token);

    /**
     * Remove all refresh tokens for a specific user
     * @param userId The user ID
     */
    void removeAllUserTokens(Integer userId);
}