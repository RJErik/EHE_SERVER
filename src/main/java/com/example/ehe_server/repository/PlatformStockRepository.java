package com.example.ehe_server.repository;

import com.example.ehe_server.entity.PlatformStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformStockRepository extends JpaRepository<PlatformStock, Integer> {
    List<PlatformStock> findByPlatformName(String platformName);
    boolean existsByPlatformName(String platformName);
}
