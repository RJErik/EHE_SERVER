package com.example.ehe_server.service.auth;

import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CookieService implements CookieServiceInterface {

    private final JwtProperties jwtConfig;

    public CookieService(JwtProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public void addJwtAccessCookie(String jwtToken, HttpServletResponse response) {
        if (jwtToken == null || jwtToken.trim().isEmpty())  {
            throw new IllegalArgumentException("Jwt access token addition to cookie failed: Jwt access token is null or empty in");
        }
        Cookie cookie = new Cookie("jwt_access_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); //turn to true after development.
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtConfig.getJwtAccessExpirationTime() / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void addJwtRefreshCookie(String jwtToken, HttpServletResponse response) {
        if (jwtToken == null || jwtToken.trim().isEmpty())  {
            throw new IllegalArgumentException("Jwt refresh token addition to cookie failed: jwtToken refresh token is null or empty");
        }
        Cookie cookie = new Cookie("jwt_refresh_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); //turn to true after development.
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtConfig.getJwtRefreshExpirationTime() / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void clearJwtCookies(HttpServletResponse response) {
        // Clear access token cookie
        Cookie accessCookie = new Cookie("jwt_access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); //turn to true after development.
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Strict");

        response.addCookie(accessCookie);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("jwt_refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);  //turn to true after development.
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");

        response.addCookie(refreshCookie);
    }
}
