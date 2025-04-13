package com.example.ehe_server.service.intf.user;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface UserLogoutServiceInterface {
    Map<String, Object> logoutUser(HttpServletResponse response);
}
