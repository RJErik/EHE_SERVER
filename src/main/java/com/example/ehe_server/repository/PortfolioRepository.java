package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    List<Portfolio> findByUser(User user);
    Optional<Portfolio> findByPortfolioIdAndUser_UserId(Integer portfolioId, Integer userId);
}
