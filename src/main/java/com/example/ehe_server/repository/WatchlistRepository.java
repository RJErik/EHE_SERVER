package com.example.ehe_server.repository;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Integer> {
    Optional<Watchlist> findByUser(User user);
}
