package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService implements CookieServiceInterface {

    @Value("${jwt.access.expiration.time}")
    private long jwtAccessExpirationTime;

    @Value("${jwt.refresh.expiration.time}")
    private long jwtRefreshExpirationTime;

    @Override
    public void addJwtAccessCookie(String jwtToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_access_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); //turn to true after development.
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtAccessExpirationTime / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void addJwtRefreshCookie(String jwtToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_refresh_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); //turn to true after development.
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtRefreshExpirationTime / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void clearJwtCookies(HttpServletResponse response) {
        // Clear access token cookie
        Cookie accessCookie = new Cookie("jwt_access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Strict");

        response.addCookie(accessCookie);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("jwt_refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");

        response.addCookie(refreshCookie);
    }

}
