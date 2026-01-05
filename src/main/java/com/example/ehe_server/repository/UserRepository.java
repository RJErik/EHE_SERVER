package com.example.ehe_server.repository;

import com.example.ehe_server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String emailHash);

    @Query("SELECT u FROM User u WHERE " +
            "(:userId IS NULL OR u.userId = :userId) AND " +
            "(:username IS NULL OR u.userName LIKE %:username%) AND " +
            "(:email IS NULL OR u.email LIKE %:email%) AND " +
            "(:accountStatus IS NULL OR u.accountStatus = :accountStatus) AND " +
            "(:registrationDateFrom IS NULL OR u.registrationDate >= :registrationDateFrom) AND " +
            "(:registrationDateTo IS NULL OR u.registrationDate <= :registrationDateTo) " +
            "ORDER BY u.registrationDate DESC")
    Page<User> searchUsers(
            @Param("userId") Integer userId,
            @Param("username") String username,
            @Param("email") String email,
            @Param("accountStatus") User.AccountStatus accountStatus,
            @Param("registrationDateFrom") LocalDateTime registrationDateFrom,
            @Param("registrationDateTo") LocalDateTime registrationDateTo,
            Pageable pageable
    );



    @Query("SELECT u FROM User u WHERE NOT EXISTS (SELECT a FROM Admin a WHERE a.user = u) ORDER BY u.registrationDate DESC")
    Page<User> findAllNonAdminsOrderByRegistrationDateDesc(Pageable pageable);
}
