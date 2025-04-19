package com.example.ehe_server.repository;

import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, Integer> {
    Optional<EmailChangeRequest> findByVerificationToken(VerificationToken verificationToken);
}
