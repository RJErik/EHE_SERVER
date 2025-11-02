package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioSearchResponse;
import java.math.BigDecimal;
import java.util.List;

public interface PortfolioSearchServiceInterface {
    List<PortfolioSearchResponse> searchPortfolios(Integer userId, String platform,
                                                   BigDecimal minValue, BigDecimal maxValue);
}
