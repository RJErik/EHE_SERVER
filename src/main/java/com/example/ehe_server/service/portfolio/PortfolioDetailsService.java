package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.CashDetails;
import com.example.ehe_server.dto.PortfolioDetailsResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.dto.StockDetails;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.intf.portfolio.PortfolioDetailsServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
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
                    String strippedSymbol = symbol.endsWith(cashCurrency)
                            ? symbol.substring(0, symbol.length() - cashCurrency.length())
                            : symbol;
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