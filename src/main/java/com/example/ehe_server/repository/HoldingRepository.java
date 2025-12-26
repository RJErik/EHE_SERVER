package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Holding;
import com.example.ehe_server.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Integer> {
    List<Holding> findByPortfolio(Portfolio portfolio);
}
