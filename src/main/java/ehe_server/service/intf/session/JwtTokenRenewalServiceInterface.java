package ehe_server.service.intf.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface JwtTokenRenewalServiceInterface {
    void renewToken(Integer userId, HttpServletRequest request, HttpServletResponse response);
}
