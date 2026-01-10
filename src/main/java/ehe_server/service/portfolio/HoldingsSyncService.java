package ehe_server.service.portfolio;

import ehe_server.dto.HoldingsUpdateResponse;
import ehe_server.entity.ApiKey;
import ehe_server.entity.Holding;
import ehe_server.entity.PlatformStock;
import ehe_server.entity.Portfolio;
import ehe_server.exception.custom.*;
import ehe_server.repository.HoldingRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.intf.portfolio.HoldingsSyncServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HoldingsSyncService implements HoldingsSyncServiceInterface {

    private static final String USDT = "USDT";
    private static final String BALANCE_KEY = "balances";
    private static final String FREE_KEY = "free";
    private static final String ASSET_KEY = "asset";

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountServiceInterface binanceAccountService;

    public HoldingsSyncService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountServiceInterface binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
    }

    private record ParsedBalance(String asset, BigDecimal quantity) {

        boolean isUsdt() {
            return USDT.equals(asset);
        }

        boolean hasPositiveBalance() {
            return quantity.compareTo(BigDecimal.ZERO) > 0;
        }

        String toTradingPair() {
            return asset + USDT;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public HoldingsUpdateResponse syncHoldings(Integer userId, Integer portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(userId, portfolioId);
        return syncHoldings(userId, portfolio);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public HoldingsUpdateResponse syncHoldings(Integer userId, Portfolio portfolio) {
        validatePortfolioOwnership(portfolio, userId);
        ApiKey apiKey = getApiKeyOrThrow(portfolio);

        List<Map<String, Object>> rawBalances = fetchBalancesFromExchange(apiKey);
        List<ParsedBalance> balances = parseBalances(rawBalances);

        return syncHoldingsWithBalances(portfolio, balances, apiKey.getPlatform().getPlatformName());
    }

    // ==================== Portfolio Validation Helpers ====================

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

    private ApiKey getApiKeyOrThrow(Portfolio portfolio) {
        ApiKey apiKey = portfolio.getApiKey();
        if (apiKey == null) {
            throw new ApiKeyMissingException(portfolio.getPortfolioId());
        }
        return apiKey;
    }

    // ==================== Exchange Communication ====================

    private List<Map<String, Object>> fetchBalancesFromExchange(ApiKey apiKey) {
        Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(
                apiKey.getApiKeyValue(),
                apiKey.getSecretKey()
        );

        if (accountInfo == null || !accountInfo.containsKey(BALANCE_KEY)) {
            throw new BinanceApiCommunicationException();
        }

        return (List<Map<String, Object>>) accountInfo.get(BALANCE_KEY);
    }

    // ==================== Balance Parsing ====================

    private List<ParsedBalance> parseBalances(List<Map<String, Object>> rawBalances) {
        return rawBalances.stream()
                .map(this::toBalance)
                .filter(ParsedBalance::hasPositiveBalance)
                .toList();
    }

    private ParsedBalance toBalance(Map<String, Object> raw) {
        String asset = (String) raw.get(ASSET_KEY);
        BigDecimal quantity = new BigDecimal((String) raw.get(FREE_KEY));
        return new ParsedBalance(asset, quantity);
    }

    private BigDecimal extractUsdtBalance(List<ParsedBalance> balances) {
        return balances.stream()
                .filter(ParsedBalance::isUsdt)
                .map(ParsedBalance::quantity)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private List<ParsedBalance> filterTradableBalances(List<ParsedBalance> balances) {
        return balances.stream()
                .filter(b -> !b.isUsdt())
                .toList();
    }

    // ==================== Holdings Synchronization ====================

    private HoldingsUpdateResponse syncHoldingsWithBalances(
            Portfolio portfolio,
            List<ParsedBalance> balances,
            String platformName) {

        BigDecimal reservedCash = extractUsdtBalance(balances);
        List<ParsedBalance> tradableBalances = filterTradableBalances(balances);

        Map<String, PlatformStock> stockMap = buildStockMap(tradableBalances, platformName);

        clearExistingHoldings(portfolio);
        int addedCount = createNewHoldings(portfolio, tradableBalances, stockMap);

        portfolio.setReservedCash(reservedCash);
        portfolioRepository.save(portfolio);

        loggingService.logAction(String.format(
                "Synced holdings for portfolio %s: added %d holdings, reserved cash: %s",
                portfolio.getPortfolioName(), addedCount, reservedCash));

        return new HoldingsUpdateResponse(addedCount, reservedCash);
    }

    private Map<String, PlatformStock> buildStockMap(List<ParsedBalance> balances, String platformName) {
        Set<String> tradingPairs = balances.stream()
                .map(ParsedBalance::toTradingPair)
                .collect(Collectors.toSet());

        return platformStockRepository
                .findByPlatformNameAndStockNameIn(platformName, tradingPairs)
                .stream()
                .collect(Collectors.toMap(
                        ps -> ps.getStock().getStockSymbol(),
                        stock -> stock
                ));
    }

    private void clearExistingHoldings(Portfolio portfolio) {
        List<Holding> existing = holdingRepository.findByPortfolio(portfolio);
        holdingRepository.deleteAll(existing);
        holdingRepository.flush();
    }

    private int createNewHoldings(
            Portfolio portfolio,
            List<ParsedBalance> balances,
            Map<String, PlatformStock> stockMap) {

        List<Holding> holdingsToSave = balances.stream()
                .map(balance -> createHoldingIfStockExists(portfolio, balance, stockMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        holdingRepository.saveAll(holdingsToSave);
        return holdingsToSave.size();
    }

    private Optional<Holding> createHoldingIfStockExists(
            Portfolio portfolio,
            ParsedBalance balance,
            Map<String, PlatformStock> stockMap) {

        PlatformStock stock = stockMap.get(balance.toTradingPair());
        if (stock == null) {
            return Optional.empty();
        }

        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setPlatformStock(stock);
        holding.setQuantity(balance.quantity());
        return Optional.of(holding);
    }
}