package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Alert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {
    @EntityGraph(attributePaths = {"platformStock", "platformStock.platform", "platformStock.stock"})
    List<Alert> findByUser_UserIdOrderByDateCreatedDesc(Integer userId);
    List<Alert> findByUser_UserId(Integer userId);

    @EntityGraph(attributePaths = {"platformStock", "platformStock.platform", "platformStock.stock"})
    @Query("SELECT a FROM Alert a WHERE " +
            "a.user.userId = :userId AND " +
            "(:platform IS NULL OR a.platformStock.platform.platformName = :platform) AND " +
            "(:symbol IS NULL OR a.platformStock.stock.stockSymbol = :symbol) AND " +
            "(:conditionType IS NULL OR a.conditionType = :conditionType) " +
            "ORDER BY a.dateCreated DESC")
    List<Alert> searchAlerts(
            @Param("userId") Integer userId,
            @Param("platform") String platform,
            @Param("symbol") String symbol,
            @Param("conditionType") Alert.ConditionType conditionType
    );
}
