package com.example.ehe_server.service.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.service.intf.stock.MarketCandleServiceInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockCandleProcessingServiceInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StockCandleProcessingService implements StockCandleProcessingServiceInterface {

    private final MarketCandleServiceInterface marketCandleService;

    public StockCandleProcessingService(MarketCandleServiceInterface marketCandleService) {
        this.marketCandleService = marketCandleService;
    }

    @Override
    public Optional<CandleData> getLatestCandle(StockCandleSubscription subscription) {
        CandleData candle = marketCandleService.getLatestCandle(
                subscription.getPlatformName(),
                subscription.getStockSymbol(),
                subscription.getTimeframe());
        return Optional.ofNullable(candle);
    }

    @Override
    public CandleUpdateResult checkForUpdates(StockCandleSubscription subscription) {
        List<CandleData> candlesToSend = new ArrayList<>();
        boolean hasUpdates;

        Optional<CandleData> currentLatestOpt = getLatestCandle(subscription);

        if (currentLatestOpt.isEmpty()) {
            return new CandleUpdateResult(false, candlesToSend, null);
        }

        CandleData currentLatest = currentLatestOpt.get();

        // Check if we have a new candle (different timestamp)
        if (isNewCandle(subscription, currentLatest)) {
            hasUpdates = handleNewCandle(subscription, currentLatest, candlesToSend);
        } else {
            hasUpdates = handlePossibleModification(subscription, candlesToSend);
        }

        return new CandleUpdateResult(hasUpdates, candlesToSend, currentLatest);
    }

    private boolean isNewCandle(StockCandleSubscription subscription, CandleData currentLatest) {
        return !currentLatest.getTimestamp().equals(subscription.getLatestCandleTimestamp());
    }

    private boolean handleNewCandle(
            StockCandleSubscription subscription,
            CandleData currentLatest,
            List<CandleData> candlesToSend) {

        // Before sending the new candle, check if the previous one was modified
        if (subscription.getLatestCandleTimestamp() != null) {
            CandleData modifiedPrevious = getModifiedCandle(subscription);
            if (modifiedPrevious != null) {
                candlesToSend.add(modifiedPrevious);
            }
        }

        // Add the new latest candle
        candlesToSend.add(currentLatest);
        return true;
    }

    private boolean handlePossibleModification(
            StockCandleSubscription subscription,
            List<CandleData> candlesToSend) {

        // Same timestamp, check if the current candle was modified
        CandleData modifiedCandle = getModifiedCandle(subscription);

        if (modifiedCandle != null) {
            candlesToSend.add(modifiedCandle);
            return true;
        }

        return false;
    }

    private CandleData getModifiedCandle(StockCandleSubscription subscription) {
        return marketCandleService.getModifiedCandle(
                subscription.getPlatformName(),
                subscription.getStockSymbol(),
                subscription.getTimeframe(),
                subscription.getLatestCandleTimestamp(),
                subscription.getLatestCandleOpen(),
                subscription.getLatestCandleHigh(),
                subscription.getLatestCandleLow(),
                subscription.getLatestCandleClose(),
                subscription.getLatestCandleVolume());
    }
}