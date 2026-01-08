package ehe_server.service.intf.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserLogoutServiceInterface {
    void logoutUser(Integer userId, HttpServletRequest request, HttpServletResponse response);
}
