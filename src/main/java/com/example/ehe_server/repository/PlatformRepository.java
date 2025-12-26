package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformRepository extends JpaRepository<Platform, Integer> {

    Optional<Platform> findByPlatformName(String platformName);

    boolean existsByPlatformName(String platformName);

    List<Platform> findAllByOrderByPlatformNameAsc();
}