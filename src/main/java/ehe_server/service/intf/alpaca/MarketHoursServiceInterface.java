package ehe_server.service.intf.alpaca;

public interface MarketHoursServiceInterface {

    /**
     * Checks if the stock market is currently open
     * Note: Crypto markets are always open (24/7)
     *
     * @return true if market is open, false otherwise
     */
    boolean isMarketOpen();
}