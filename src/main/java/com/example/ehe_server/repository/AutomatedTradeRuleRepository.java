package com.example.ehe_server.repository;

import com.example.ehe_server.entity.AutomatedTradeRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AutomatedTradeRuleRepository extends JpaRepository<AutomatedTradeRule, Integer> {
    @EntityGraph(attributePaths = {"portfolio", "platformStock"})
    List<AutomatedTradeRule> findByUser_UserIdOrderByDateCreatedDesc(Integer userId);

    @EntityGraph(attributePaths = {"portfolio", "platformStock"})
    @Query("SELECT atr FROM AutomatedTradeRule atr WHERE " +
            "atr.user.userId = :userId AND " +
            "(:portfolioId IS NULL OR atr.portfolio.portfolioId = :portfolioId) AND " +
            "(:platform IS NULL OR atr.platformStock.platformName = :platform) AND " +
            "(:symbol IS NULL OR atr.platformStock.stockSymbol = :symbol) AND " +
            "(:conditionType IS NULL OR atr.conditionType = :conditionType) AND " +
            "(:actionType IS NULL OR atr.actionType = :actionType) AND " +
            "(:quantityType IS NULL OR atr.quantityType = :quantityType) AND " +
            "(:minThresholdValue IS NULL OR atr.thresholdValue >= :minThresholdValue) AND " +
            "(:maxThresholdValue IS NULL OR atr.thresholdValue <= :maxThresholdValue) " +
            "ORDER BY atr.dateCreated DESC")
    List<AutomatedTradeRule> searchAutomatedTradeRules(
            @Param("userId") Integer userId,
            @Param("portfolioId") Integer portfolioId,
            @Param("platform") String platform,
            @Param("symbol") String symbol,
            @Param("conditionType") AutomatedTradeRule.ConditionType conditionType,
            @Param("actionType") AutomatedTradeRule.ActionType actionType,
            @Param("quantityType") AutomatedTradeRule.QuantityType quantityType,
            @Param("minThresholdValue") BigDecimal minThresholdValue,
            @Param("maxThresholdValue") BigDecimal maxThresholdValue
    );
}