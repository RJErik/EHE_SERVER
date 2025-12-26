package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {

    Optional<Stock> findByStockName(String stockName);

    boolean existsByStockName(String stockName);

    List<Stock> findAllByOrderByStockNameAsc();

    List<Stock> findByStockNameIn(Set<String> stockNames);

    List<Stock> findByStockNameContainingIgnoreCase(String stockName);
}