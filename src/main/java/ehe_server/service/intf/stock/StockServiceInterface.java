package ehe_server.service.intf.stock;

import ehe_server.dto.StocksByPlatformResponse;

public interface StockServiceInterface {
    /**
     * Retrieves all stocks available for a specific platform
     * @param platformName The name of the platform
     * @return Map containing success status, message and stocks list
     */
    StocksByPlatformResponse getStocksByPlatform(String platformName);
}
