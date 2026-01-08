package ehe_server.repository;

import ehe_server.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    List<ApiKey> findByUser_UserIdOrderByDateAddedDesc(Integer userId);
}
