package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {
    List<Alert> findByUser_UserIdAndActiveTrue(Integer userId);
    List<Alert> findByUser_UserIdAndPlatformStock_PlatformNameAndActiveTrue(Integer userId, String platform);
    List<Alert> findByUser_UserIdAndPlatformStock_StockSymbolAndActiveTrue(Integer userId, String symbol);
    List<Alert> findByUser_UserIdAndConditionTypeAndActiveTrue(Integer userId, Alert.ConditionType conditionType);
    List<Alert> findByUser_UserIdAndPlatformStock_PlatformNameAndPlatformStock_StockSymbolAndActiveTrue(Integer userId, String platform, String symbol);
    List<Alert> findByUser_UserIdAndPlatformStock_PlatformNameAndConditionTypeAndActiveTrue(Integer userId, String platform, Alert.ConditionType conditionType);
    List<Alert> findByUser_UserIdAndPlatformStock_StockSymbolAndConditionTypeAndActiveTrue(Integer userId, String symbol, Alert.ConditionType conditionType);
    List<Alert> findByUser_UserIdAndPlatformStock_PlatformNameAndPlatformStock_StockSymbolAndConditionTypeAndActiveTrue(Integer userId, String platform, String symbol, Alert.ConditionType conditionType);
}
