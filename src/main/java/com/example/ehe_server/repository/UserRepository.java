package com.example.ehe_server.repository;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String emailHash);

    @Query("SELECT u FROM User u WHERE " +
            "NOT EXISTS (SELECT a FROM Admin a WHERE a.user = u) AND " + // Exclude admins
            "(:userId IS NULL OR u.userId = :userId) AND " +
            "(:userName IS NULL OR u.userName = :userName) AND " +
            "(:email IS NULL OR u.email = :email) AND " +
            "(:accountStatus IS NULL OR u.accountStatus = :accountStatus) AND " +
            "(:registrationDateFromTime IS NULL OR u.registrationDate >= :registrationDateFromTime) AND " +
            "(:registrationDateToTime IS NULL OR u.registrationDate <= :registrationDateToTime) " +
            "ORDER BY u.registrationDate DESC")
    List<User> searchUsers(@Param("userId") Integer userId,
                           @Param("userName") String userName,
                           @Param("email") String email,
                           @Param("accountStatus") User.AccountStatus accountStatus,
                           @Param("registrationDateFromTime") LocalDateTime registrationDateFromTime,
                           @Param("registrationDateToTime") LocalDateTime registrationDateToTime);


    @Query("SELECT u FROM User u WHERE NOT EXISTS (SELECT a FROM Admin a WHERE a.user = u) ORDER BY u.registrationDate DESC")
    List<User> findAllNonAdminsOrderByRegistrationDateDesc();

}
