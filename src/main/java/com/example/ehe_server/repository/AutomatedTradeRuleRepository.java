package com.example.ehe_server.repository;

import com.example.ehe_server.entity.AutomatedTradeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AutomatedTradeRuleRepository extends JpaRepository<AutomatedTradeRule, Integer> {
    List<AutomatedTradeRule> findByUser_UserIdAndIsActiveTrue(Integer userId);
    List<AutomatedTradeRule> findByPortfolio_PortfolioIdAndIsActiveTrue(Integer portfolioId);
    List<AutomatedTradeRule> findByPlatformStock_PlatformNameAndIsActiveTrue(String platformName);
    List<AutomatedTradeRule> findByPlatformStock_StockSymbolAndIsActiveTrue(String stockSymbol);
    List<AutomatedTradeRule> findByActionTypeAndIsActiveTrue(AutomatedTradeRule.ActionType actionType);
    List<AutomatedTradeRule> findByQuantityTypeAndIsActiveTrue(AutomatedTradeRule.QuantityType quantityType);
    List<AutomatedTradeRule> findByThresholdValueGreaterThanEqualAndIsActiveTrue(BigDecimal minThreshold);
    List<AutomatedTradeRule> findByThresholdValueLessThanEqualAndIsActiveTrue(BigDecimal maxThreshold);
    List<AutomatedTradeRule> findByConditionTypeAndIsActiveTrue(AutomatedTradeRule.ConditionType conditionType);
    List<AutomatedTradeRule> findAllByIsActiveTrue();
    List<AutomatedTradeRule> findByUser_UserIdAndPortfolio_PortfolioIdAndIsActiveTrue(Integer userId, Integer portfolioId);
}