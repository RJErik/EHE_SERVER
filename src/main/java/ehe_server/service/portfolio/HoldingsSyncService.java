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
import ehe_server.service.intf.alpaca.AlpacaAccountServiceInterface;
import ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.intf.portfolio.HoldingsSyncServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HoldingsSyncService implements HoldingsSyncServiceInterface {

    private static final String BINANCE_PLATFORM = "Binance";
    private static final String BINANCE_CASH_CURRENCY = "USDT";
    private static final String BINANCE_BALANCE_KEY = "balances";
    private static final String BINANCE_ASSET_KEY = "asset";
    private static final String BINANCE_FREE_KEY = "free";

    private static final String ALPACA_PLATFORM = "Alpaca";
    private static final String ALPACA_CASH_CURRENCY = "USD";
    private static final String ALPACA_CASH_KEY = "cash";
    private static final String ALPACA_POSITIONS_KEY = "positions";
    private static final String ALPACA_SYMBOL_KEY = "symbol";
    private static final String ALPACA_QTY_KEY = "qty";

    private static final int DECIMAL_SCALE = 8;

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountServiceInterface binanceAccountService;
    private final AlpacaAccountServiceInterface alpacaAccountService;

    public HoldingsSyncService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountServiceInterface binanceAccountService,
            AlpacaAccountServiceInterface alpacaAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
        this.alpacaAccountService = alpacaAccountService;
    }


    /**
     * Rounds BigDecimal to 8 decimal places for database compatibility.
     */
    private BigDecimal scaleDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private record ParsedBalance(String asset, BigDecimal quantity, String platformName) {

        boolean isCash() {
            return BINANCE_CASH_CURRENCY.equals(asset) || ALPACA_CASH_CURRENCY.equals(asset);
        }

        boolean hasPositiveBalance() {
            return quantity.compareTo(BigDecimal.ZERO) > 0;
        }

        /**
         * Converts asset to database trading pair format
         * Binance: BTC → BTCUSDT
         * Alpaca Crypto: BTCUSD → BTC/USD
         * Alpaca Stock: AAPL → AAPL
         */
        String toDatabaseSymbol() {
            if (BINANCE_PLATFORM.equals(platformName)) {
                return asset + BINANCE_CASH_CURRENCY;
            } else if (ALPACA_PLATFORM.equals(platformName)) {
                if (asset.endsWith(ALPACA_CASH_CURRENCY) && asset.length() > 3) {
                    String base = asset.substring(0, asset.length() - 3);
                    return base + "/" + ALPACA_CASH_CURRENCY;
                }
                return asset;
            }
            return asset;
        }
    }

    @Override
    public HoldingsUpdateResponse syncHoldings(Integer userId, Integer portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(userId, portfolioId);
        return syncHoldings(userId, portfolio);
    }

    @Override
    public HoldingsUpdateResponse syncHoldings(Integer userId, Portfolio portfolio) {
        validatePortfolioOwnership(portfolio, userId);
        ApiKey apiKey = getApiKeyOrThrow(portfolio);

        String platformName = apiKey.getPlatform().getPlatformName();
        List<ParsedBalance> balances = fetchAndParseBalances(apiKey, platformName);

        return syncHoldingsWithBalances(portfolio, balances, platformName);
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

    private ApiKey getApiKeyOrThrow(Portfolio portfolio) {
        ApiKey apiKey = portfolio.getApiKey();
        if (apiKey == null) {
            throw new ApiKeyMissingException(portfolio.getPortfolioId());
        }
        return apiKey;
    }

    private List<ParsedBalance> fetchAndParseBalances(ApiKey apiKey, String platformName) {
        if (BINANCE_PLATFORM.equals(platformName)) {
            return fetchAndParseBinanceBalances(apiKey);
        } else if (ALPACA_PLATFORM.equals(platformName)) {
            return fetchAndParseAlpacaBalances(apiKey);
        } else {
            throw new UnsupportedPlatformException(platformName);
        }
    }

    private List<ParsedBalance> fetchAndParseBinanceBalances(ApiKey apiKey) {
        Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(
                apiKey.getApiKeyValue(),
                apiKey.getSecretKey()
        );

        if (accountInfo == null || !accountInfo.containsKey(BINANCE_BALANCE_KEY)) {
            throw new BinanceApiCommunicationException();
        }

        List<Map<String, Object>> rawBalances =
                (List<Map<String, Object>>) accountInfo.get(BINANCE_BALANCE_KEY);

        return rawBalances.stream()
                .map(this::toBinanceBalance)
                .filter(ParsedBalance::hasPositiveBalance)
                .toList();
    }

    private ParsedBalance toBinanceBalance(Map<String, Object> raw) {
        String asset = (String) raw.get(BINANCE_ASSET_KEY);
        BigDecimal quantity = new BigDecimal((String) raw.get(BINANCE_FREE_KEY));
        return new ParsedBalance(asset, quantity, BINANCE_PLATFORM);
    }

    private List<ParsedBalance> fetchAndParseAlpacaBalances(ApiKey apiKey) {
        Map<String, Object> accountInfo = alpacaAccountService.getAccountInfo(
                apiKey.getApiKeyValue(),
                apiKey.getSecretKey()
        );

        if (accountInfo == null) {
            throw new AlpacaApiCommunicationException();
        }

        List<ParsedBalance> balances = new ArrayList<>();

        // Extract cash balance from account info
        if (accountInfo.containsKey(ALPACA_CASH_KEY)) {
            String cashStr = (String) accountInfo.get(ALPACA_CASH_KEY);
            BigDecimal cashAmount = new BigDecimal(cashStr);
            if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
                balances.add(new ParsedBalance(ALPACA_CASH_CURRENCY, cashAmount, ALPACA_PLATFORM));
            }
        }

        // Extract positions if present
        if (accountInfo.containsKey(ALPACA_POSITIONS_KEY)) {
            List<Map<String, Object>> positions =
                    (List<Map<String, Object>>) accountInfo.get(ALPACA_POSITIONS_KEY);

            for (Map<String, Object> position : positions) {
                ParsedBalance balance = toAlpacaBalance(position);
                if (balance.hasPositiveBalance()) {
                    balances.add(balance);
                }
            }
        }

        return balances;
    }

    private ParsedBalance toAlpacaBalance(Map<String, Object> raw) {
        String symbol = (String) raw.get(ALPACA_SYMBOL_KEY);
        Object qtyObj = raw.get(ALPACA_QTY_KEY);
        BigDecimal quantity;

        if (qtyObj instanceof String) {
            quantity = new BigDecimal((String) qtyObj);
        } else if (qtyObj instanceof Number) {
            quantity = new BigDecimal(qtyObj.toString());
        } else {
            quantity = BigDecimal.ZERO;
        }

        return new ParsedBalance(symbol, quantity, ALPACA_PLATFORM);
    }

    private BigDecimal extractCashBalance(List<ParsedBalance> balances) {
        return balances.stream()
                .filter(ParsedBalance::isCash)
                .map(ParsedBalance::quantity)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private List<ParsedBalance> filterTradableBalances(List<ParsedBalance> balances) {
        return balances.stream()
                .filter(b -> !b.isCash())
                .toList();
    }

    private HoldingsUpdateResponse syncHoldingsWithBalances(
            Portfolio portfolio,
            List<ParsedBalance> balances,
            String platformName) {

        BigDecimal reservedCash = scaleDecimal(extractCashBalance(balances));
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
        Set<String> databaseSymbols = balances.stream()
                .map(ParsedBalance::toDatabaseSymbol)
                .collect(Collectors.toSet());

        return platformStockRepository
                .findByPlatformNameAndStockNameIn(platformName, databaseSymbols)
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

        PlatformStock stock = stockMap.get(balance.toDatabaseSymbol());
        if (stock == null) {
            return Optional.empty();
        }

        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setPlatformStock(stock);
        holding.setQuantity(scaleDecimal(balance.quantity()));
        return Optional.of(holding);
    }
}