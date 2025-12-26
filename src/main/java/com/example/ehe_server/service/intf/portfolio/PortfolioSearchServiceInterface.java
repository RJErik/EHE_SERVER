package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioResponse;
import java.math.BigDecimal;
import java.util.List;

public interface PortfolioSearchServiceInterface {
    List<PortfolioResponse> searchPortfolios(Integer userId, String platform,
                                             BigDecimal minValue, BigDecimal maxValue);
}
