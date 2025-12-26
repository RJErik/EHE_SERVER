package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.service.auth.JwtClaimService;

public interface JwtClaimServiceInterface {

    /**
     * Parses the JWT and extracts all necessary claims (ID and Role) in one operation.
     * Returns null if the token is invalid, expired, or empty.
     */
    JwtClaimService.TokenDetails parseTokenDetails(String token);

}