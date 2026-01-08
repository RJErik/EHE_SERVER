package ehe_server.repository;

import ehe_server.entity.Portfolio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    Optional<Portfolio> findByPortfolioIdAndUser_UserId(Integer portfolioId, Integer userId);
    List<Portfolio> findByUser_UserIdAndApiKey_Platform_PlatformNameIgnoreCaseOrderByCreationDateDesc(Integer userId, String platformName);
    boolean existsByUser_UserIdAndPortfolioNameIgnoreCase(Integer userId, String portfolioName);
    List<Portfolio> findByUser_UserIdOrderByCreationDateDesc(Integer userId);

    @EntityGraph(attributePaths = {"apiKey", "apiKey.platform"})
    @Query("SELECT p FROM Portfolio p WHERE " +
            "p.user.userId = :userId AND " +
            "(:platform IS NULL OR p.apiKey.platform.platformName = :platform) " +
            "ORDER BY p.creationDate DESC")
    List<Portfolio> searchPortfolios(
            @Param("userId") Integer userId,
            @Param("platform") String platform
    );
}