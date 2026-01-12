package ehe_server.service.portfolio;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.CashDetails;
import ehe_server.dto.PortfolioDetailsResponse;
import ehe_server.dto.PortfolioValueResponse;
import ehe_server.dto.StockDetails;
import ehe_server.entity.ApiKey;
import ehe_server.entity.Portfolio;
import ehe_server.exception.custom.PortfolioNotFoundException;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.portfolio.PortfolioDetailsServiceInterface;
import ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class PortfolioDetailsService implements PortfolioDetailsServiceInterface {

    private static final String USDT_CURRENCY = "USDT";
    private static final String USD_CURRENCY = "USD";
    private static final Set<String> CRYPTO_EXCHANGES = Set.of("Binance");

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueServiceInterface portfolioValueService;

    public PortfolioDetailsService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.details",
            params = {
                    "#portfolioId",
                    "#result.name",
                    "#result.platform",
                    "#result.creationDate",
                    "#result.stocks.size()",
                    "#result.totalValue",
            }
    )
    @Override
    public PortfolioDetailsResponse getPortfolioDetails(Integer userId, Integer portfolioId) {

        PortfolioValueResponse valueResponse =
                portfolioValueService.calculatePortfolioValue(userId, portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        ApiKey apiKey = portfolio.getApiKey();
        String platformName = apiKey.getPlatform().getPlatformName();

        // Determine currency based on whether it's a crypto exchange
        boolean isCryptoExchange = CRYPTO_EXCHANGES.stream()
                .anyMatch(exchange -> exchange.equalsIgnoreCase(platformName));
        String cashCurrency = isCryptoExchange ? USDT_CURRENCY : USD_CURRENCY;

        List<StockDetails> stocksList = valueResponse.getHoldings().stream()
                .map(h -> {
                    String symbol = h.getSymbol();
                    String strippedSymbol = symbol;

                    if (symbol.endsWith(cashCurrency)) {
                        strippedSymbol = symbol.substring(0, symbol.length() - cashCurrency.length());
                        if (strippedSymbol.endsWith("/")) {
                            strippedSymbol = strippedSymbol.substring(0, strippedSymbol.length() - 1);
                        }
                    }

                    return new StockDetails(strippedSymbol, h.getValueInUsdt());
                })
                .toList();

        CashDetails cashDetails = new CashDetails(cashCurrency, valueResponse.getReservedCash());

        return new PortfolioDetailsResponse(
                valueResponse.getPortfolioName(),
                portfolio.getCreationDate(),
                platformName,
                cashDetails,
                stocksList,
                valueResponse.getTotalValue()
        );
    }
}