package ehe_server.service.auth;

import ehe_server.exception.custom.MissingJwtAccessTokenException;
import ehe_server.exception.custom.MissingJwtRefreshTokenException;
import ehe_server.properties.JwtProperties;
import ehe_server.service.intf.auth.CookieServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CookieService implements CookieServiceInterface {

    @Value("${spring.security.secure-cookie}")
    private boolean requireHttps;

    private final JwtProperties jwtConfig;

    public CookieService(JwtProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public void addJwtAccessCookie(String jwtToken, HttpServletResponse response) {
        if (jwtToken == null || jwtToken.trim().isEmpty())  {
            throw new MissingJwtAccessTokenException();
        }
        Cookie cookie = new Cookie("jwt_access_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(requireHttps);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtConfig.getJwtAccessExpirationTime() / 1000));
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    @Override
    public void addJwtRefreshCookie(String jwtToken, HttpServletResponse response) {
        if (jwtToken == null || jwtToken.trim().isEmpty())  {
            throw new MissingJwtRefreshTokenException();
        }
        Cookie cookie = new Cookie("jwt_refresh_token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(requireHttps);
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
        accessCookie.setSecure(requireHttps);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Strict");

        response.addCookie(accessCookie);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("jwt_refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(requireHttps);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");

        response.addCookie(refreshCookie);
    }
}
