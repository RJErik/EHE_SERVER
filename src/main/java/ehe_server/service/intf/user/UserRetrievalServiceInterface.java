package ehe_server.service.intf.user;

import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.UserResponse;

public interface UserRetrievalServiceInterface {
    PaginatedResponse<UserResponse> getUsers(Integer size, Integer page);
}
