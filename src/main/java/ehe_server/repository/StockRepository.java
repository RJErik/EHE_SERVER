package ehe_server.repository;

import ehe_server.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {
    Optional<Stock> findByStockSymbol(String stockSymbol);
}