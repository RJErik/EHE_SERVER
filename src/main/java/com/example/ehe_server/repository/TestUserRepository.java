package com.example.ehe_server.repository;

import com.example.ehe_server.DatabaseEntities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestUserRepository extends JpaRepository<User, Integer> {
}
