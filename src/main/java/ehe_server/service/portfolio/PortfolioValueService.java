package ehe_server.service.portfolio;

import ehe_server.dto.HoldingDetails;
import ehe_server.dto.PortfolioValueResponse;
import ehe_server.entity.Holding;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import ehe_server.entity.Portfolio;
import ehe_server.exception.custom.PortfolioNotFoundException;
import ehe_server.exception.custom.UnauthorizedPortfolioAccessException;
import ehe_server.repository.HoldingRepository;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.intf.portfolio.HoldingsSyncServiceInterface;
import ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PortfolioValueService implements PortfolioValueServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final HoldingsSyncServiceInterface holdingsSyncService;

    private static final String USDT = "USDT";
    private static final Set<String> CRYPTO_EXCHANGES = Set.of("Binance");

    private static final int DECIMAL_SCALE = 2;

    public PortfolioValueService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            HoldingsSyncServiceInterface holdingsSyncService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.holdingsSyncService = holdingsSyncService;
    }


    @Override
    @Transactional
    public PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(userId, portfolioId);

        holdingsSyncService.syncHoldings(userId, portfolio);

        portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);

        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
        List<HoldingDetails> holdingsList = buildHoldingDetails(holdings);

        BigDecimal reservedCash = Optional.ofNullable(portfolio.getReservedCash())
                .orElse(BigDecimal.ZERO);

        BigDecimal totalValue = calculateTotalValue(holdingsList, reservedCash);

        loggingService.logAction(String.format(
                "Calculated portfolio value for %s: %s USDT",
                portfolio.getPortfolioName(), totalValue));

        return new PortfolioValueResponse(
                portfolioId,
                portfolio.getPortfolioName(),
                totalValue,
                reservedCash.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP),
                holdingsList
        );
    }

    private Portfolio getPortfolioOrThrow(Integer userId, Integer portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        validatePortfolioOwnership(portfolio, userId);

        return portfolio;
    }

    private void validatePortfolioOwnership(Portfolio portfolio, Integer userId) {
        if (!portfolio.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedPortfolioAccessException(userId, portfolio.getPortfolioId());
        }
    }

    private List<HoldingDetails> buildHoldingDetails(List<Holding> holdings) {
        return holdings.stream()
                .map(this::toHoldingDetails)
                .toList();
    }

    private HoldingDetails toHoldingDetails(Holding holding) {
        String symbol = holding.getPlatformStock().getStock().getStockSymbol();
        BigDecimal valueInUsdt = calculateHoldingValueInUsdt(holding);

        return new HoldingDetails(
                holding.getHoldingId(),
                symbol,
                holding.getQuantity(),
                valueInUsdt.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateHoldingValueInUsdt(Holding holding) {
        PlatformStock stock = holding.getPlatformStock();
        String platformName = stock.getPlatform().getPlatformName();
        String symbol = stock.getStock().getStockSymbol();

        // For Alpaca and other USD-based platforms, use the symbol directly
        // For Binance, ensure we have a USDT pair
        String priceSymbol;
        if (CRYPTO_EXCHANGES.contains(platformName)) {
            priceSymbol = symbol.endsWith(USDT) ? symbol : symbol + USDT;
        } else {
            priceSymbol = symbol;
        }

        return platformStockRepository
                .findByPlatformPlatformNameAndStockStockSymbol(platformName, priceSymbol)
                .stream()
                .findFirst()
                .map(this::getLatestPrice)
                .map(price -> holding.getQuantity().multiply(price))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getLatestPrice(PlatformStock usdtPair) {
        MarketCandle candle = marketCandleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        usdtPair, MarketCandle.Timeframe.M1);
        return candle != null ? candle.getClosePrice() : BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalValue(List<HoldingDetails> holdings, BigDecimal reservedCash) {
        return holdings.stream()
                .map(HoldingDetails::getValueInUsdt)
                .reduce(reservedCash, BigDecimal::add)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}