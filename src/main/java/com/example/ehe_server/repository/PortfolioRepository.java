package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    List<Portfolio> findByUser(User user);
    Optional<Portfolio> findByPortfolioIdAndUser_UserId(Integer portfolioId, Integer userId);

    @Query("SELECT p FROM Portfolio p WHERE " +
            "p.user.userId = :userId AND " +
            "(:platform IS NULL OR p.apiKey.platformName = :platform)")
    List<Portfolio> searchPortfolios(
            @Param("userId") Integer userId,
            @Param("platform") String platform
    );
}