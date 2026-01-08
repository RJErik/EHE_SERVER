package ehe_server.repository;

import ehe_server.entity.EmailChangeRequest;
import ehe_server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, Integer> {
    Optional<EmailChangeRequest> findByVerificationToken(VerificationToken verificationToken);
}
