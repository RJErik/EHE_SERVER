package ehe_server.repository;

import ehe_server.entity.Holding;
import ehe_server.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Integer> {
    List<Holding> findByPortfolio(Portfolio portfolio);
}
