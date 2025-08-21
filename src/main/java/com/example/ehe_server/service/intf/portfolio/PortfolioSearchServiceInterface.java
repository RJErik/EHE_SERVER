package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioSearchResponse;
import com.example.ehe_server.entity.Portfolio.PortfolioType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PortfolioSearchServiceInterface {
    List<PortfolioSearchResponse> searchPortfolios(Integer userId, PortfolioType type, String platform,
                                                   BigDecimal minValue, BigDecimal maxValue);
}
