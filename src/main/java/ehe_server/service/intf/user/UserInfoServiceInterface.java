package ehe_server.service.intf.user;

import ehe_server.dto.UserInfoResponse;

public interface UserInfoServiceInterface {
    /**
     * Retrieves basic user information (name and email) for the specified user ID
     * @param userId The ID of the user whose information should be retrieved
     * @return A map containing the operation result and user information if successful
     */
    UserInfoResponse getUserInfo(Integer userId);
}
