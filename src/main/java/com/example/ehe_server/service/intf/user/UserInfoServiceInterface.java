package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface UserInfoServiceInterface {
    /**
     * Retrieves basic user information (name and email) for the specified user ID
     * @param userId The ID of the user whose information should be retrieved
     * @return A map containing the operation result and user information if successful
     */
    Map<String, Object> getUserInfo(Long userId);
}
