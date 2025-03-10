package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService implements CookieServiceInterface {

    @Value("${jwt.expiration.time}")
    private long jwtExpirationTime;

    @Override
    public void createJwtCookie(String jwtToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationTime / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}
