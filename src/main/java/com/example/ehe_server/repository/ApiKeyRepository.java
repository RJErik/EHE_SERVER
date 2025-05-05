package com.example.ehe_server.repository;

import com.example.ehe_server.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    List<ApiKey> findByUser_UserId(Integer userId);
    Optional<ApiKey> findByApiKeyIdAndUser_UserId(Integer apiKeyId, Integer userId);
}
