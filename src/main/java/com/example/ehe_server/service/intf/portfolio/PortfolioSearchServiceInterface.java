package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.entity.Portfolio.PortfolioType;
import java.math.BigDecimal;
import java.util.Map;

public interface PortfolioSearchServiceInterface {
    Map<String, Object> searchPortfolios(PortfolioType type, String platform,
                                         BigDecimal minValue, BigDecimal maxValue);
}
