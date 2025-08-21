package com.example.ehe_server.service.intf.stock;

import java.util.List;
import java.util.Map;

public interface PlatformServiceInterface {
    /**
     * Retrieves all available trading platforms
     * @return Map containing success status, message and platforms list
     */
    List<String> getAllPlatforms();
}
