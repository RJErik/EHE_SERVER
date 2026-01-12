package ehe_server.service.trade;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.TradeExecutionResponse;
import ehe_server.entity.*;
import ehe_server.exception.custom.PlatformStockNotFoundException;
import ehe_server.exception.custom.PortfolioNotFoundException;
import ehe_server.exception.custom.UnsupportedPlatformException;
import ehe_server.repository.PortfolioRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.service.intf.alpaca.AlpacaAccountServiceInterface;
import ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import ehe_server.service.intf.trade.TradingServiceInterface;
import ehe_server.service.intf.trade.TransactionPersistenceServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Transactional
public class TradingService implements TradingServiceInterface {

    private static final String BINANCE_PLATFORM = "Binance";
    private static final String ALPACA_PLATFORM = "Alpaca";
    private static final int QUANTITY_PRECISION = 8;

    private final PortfolioRepository portfolioRepository;
    private final BinanceAccountServiceInterface binanceAccountService;
    private final AlpacaAccountServiceInterface alpacaAccountService;
    private final PlatformStockRepository platformStockRepository;
    private final TransactionPersistenceServiceInterface transactionPersistenceService;

    public TradingService(
            PortfolioRepository portfolioRepository,
            BinanceAccountServiceInterface binanceAccountService,
            AlpacaAccountServiceInterface alpacaAccountService,
            PlatformStockRepository platformStockRepository,
            TransactionPersistenceServiceInterface transactionPersistenceService) {
        this.portfolioRepository = portfolioRepository;
        this.binanceAccountService = binanceAccountService;
        this.alpacaAccountService = alpacaAccountService;
        this.platformStockRepository = platformStockRepository;
        this.transactionPersistenceService = transactionPersistenceService;
    }

    private record ParsedOrderResult(
            BigDecimal executedQty,
            BigDecimal averagePrice,
            Transaction.Status status
    ) {}

    @LogMessage(
            messageKey = "log.message.trade.execute",
            params = {
                    "#result.transactionId",
                    "#result.stockSymbol",
                    "#result.transactionType",
                    "#result.quantity",
                    "#result.status"
            }
    )
    @Override
    public TradeExecutionResponse executeTrade(Integer userId, Integer portfolioId, String stockSymbol,
                                               Transaction.TransactionType action,
                                               BigDecimal amount, AutomatedTradeRule.QuantityType quantityType) {
        Portfolio portfolio = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        ApiKey apiKey = portfolio.getApiKey();
        String platformName = apiKey.getPlatform().getPlatformName();

        PlatformStock platformStock = platformStockRepository
                .findByStockNameAndPlatformName(stockSymbol, platformName)
                .orElseThrow(() -> new PlatformStockNotFoundException(stockSymbol, platformName));

        BigDecimal quantity = amount.setScale(QUANTITY_PRECISION, RoundingMode.DOWN);

        Transaction transaction;

        try {
            Map<String, Object> orderResult = executeOrderOnPlatform(
                    apiKey, platformName, stockSymbol, action, quantity, quantityType);

            ParsedOrderResult parsedResult = parseOrderResult(orderResult, platformName);

            transaction = transactionPersistenceService.saveTransaction(
                    portfolio,
                    platformStock,
                    action,
                    parsedResult.executedQty(),
                    parsedResult.averagePrice(),
                    parsedResult.status()
            );

            return new TradeExecutionResponse(
                    transaction.getTransactionId(),
                    transaction.getPlatformStock().getStock().getStockSymbol(),
                    transaction.getTransactionType(),
                    transaction.getQuantity(),
                    transaction.getStatus()
            );

        } catch (Exception e) {
            transactionPersistenceService.saveTransaction(
                    portfolio, platformStock, action, BigDecimal.ZERO, BigDecimal.ZERO, Transaction.Status.FAILED);

            throw e;
        }
    }

    private Map<String, Object> executeOrderOnPlatform(
            ApiKey apiKey,
            String platformName,
            String stockSymbol,
            Transaction.TransactionType action,
            BigDecimal quantity,
            AutomatedTradeRule.QuantityType quantityType) {

        String apiKeyValue = apiKey.getApiKeyValue();
        String secretKey = apiKey.getSecretKey();

        if (BINANCE_PLATFORM.equals(platformName)) {
            String binanceSymbol = toBinanceSymbol(stockSymbol);
            return binanceAccountService.placeMarketOrder(
                    apiKeyValue, secretKey, binanceSymbol, action.toString(), "MARKET", quantity, quantityType);
        }

        if (ALPACA_PLATFORM.equals(platformName)) {
            String alpacaSymbol = toAlpacaSymbol(stockSymbol);
            return alpacaAccountService.placeMarketOrder(
                    apiKeyValue, secretKey, alpacaSymbol, action.toString(), "market", quantity, quantityType);
        }

        throw new UnsupportedPlatformException(platformName);
    }

    private String toBinanceSymbol(String stockSymbol) {
        if (!stockSymbol.endsWith("USDT")) {
            return stockSymbol + "USDT";
        }
        return stockSymbol;
    }

    private String toAlpacaSymbol(String stockSymbol) {
        if (stockSymbol.contains("/")) {
            return stockSymbol;
        }

        if (stockSymbol.endsWith("USD") && stockSymbol.length() > 3) {
            String base = stockSymbol.substring(0, stockSymbol.length() - 3);
            return base + "/USD";
        }

        return stockSymbol;
    }

    private ParsedOrderResult parseOrderResult(Map<String, Object> orderResult, String platformName) {
        if (BINANCE_PLATFORM.equals(platformName)) {
            return parseBinanceOrderResult(orderResult);
        }

        if (ALPACA_PLATFORM.equals(platformName)) {
            return parseAlpacaOrderResult(orderResult);
        }

        throw new UnsupportedPlatformException(platformName);
    }

    private ParsedOrderResult parseBinanceOrderResult(Map<String, Object> orderResult) {
        String orderStatus = (String) orderResult.get("status");

        Transaction.Status status = mapBinanceStatus(orderStatus);

        if (status == Transaction.Status.PENDING) {
            return new ParsedOrderResult(BigDecimal.ZERO, BigDecimal.ZERO, status);
        }

        BigDecimal executedQty = orderResult.get("executedQty") != null ?
                new BigDecimal(orderResult.get("executedQty").toString()) : BigDecimal.ZERO;

        BigDecimal cumulativeQuoteQty = orderResult.get("cummulativeQuoteQty") != null ?
                new BigDecimal(orderResult.get("cummulativeQuoteQty").toString()) : BigDecimal.ZERO;

        BigDecimal averagePrice = BigDecimal.ZERO;
        if (executedQty.compareTo(BigDecimal.ZERO) > 0) {
            averagePrice = cumulativeQuoteQty.divide(executedQty, 8, RoundingMode.HALF_UP);
        }

        return new ParsedOrderResult(executedQty, averagePrice, status);
    }

    private ParsedOrderResult parseAlpacaOrderResult(Map<String, Object> orderResult) {
        String orderStatus = (String) orderResult.get("status");

        Transaction.Status status = mapAlpacaStatus(orderStatus);

        if (status == Transaction.Status.PENDING) {
            return new ParsedOrderResult(BigDecimal.ZERO, BigDecimal.ZERO, status);
        }

        BigDecimal filledQty = BigDecimal.ZERO;
        Object filledQtyObj = orderResult.get("filled_qty");
        if (filledQtyObj != null) {
            filledQty = new BigDecimal(filledQtyObj.toString());
        }

        BigDecimal averagePrice = BigDecimal.ZERO;
        Object avgPriceObj = orderResult.get("filled_avg_price");
        if (avgPriceObj != null && !avgPriceObj.toString().isEmpty()) {
            averagePrice = new BigDecimal(avgPriceObj.toString());
        }

        return new ParsedOrderResult(filledQty, averagePrice, status);
    }

    private Transaction.Status mapBinanceStatus(String binanceStatus) {
        if (binanceStatus == null) {
            return Transaction.Status.PENDING;
        }
        return switch (binanceStatus) {
            case "FILLED" -> Transaction.Status.COMPLETED;
            case "REJECTED", "EXPIRED", "CANCELED" -> Transaction.Status.FAILED;
            default -> Transaction.Status.PENDING;
        };
    }

    private Transaction.Status mapAlpacaStatus(String alpacaStatus) {
        if (alpacaStatus == null) {
            return Transaction.Status.PENDING;
        }
        return switch (alpacaStatus) {
            case "filled" -> Transaction.Status.COMPLETED;
            case "rejected", "expired", "canceled" -> Transaction.Status.FAILED;
            default -> Transaction.Status.PENDING;
        };
    }
}