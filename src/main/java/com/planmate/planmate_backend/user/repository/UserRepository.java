package com.planmate.planmate_backend.user.repository;

import com.planmate.planmate_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends  JpaRepository<User, Long> {

    Optional<User> findByKakaoId(Long kakaoId);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken, u.salt = :salt WHERE u.id = :userId")
    void updateRefreshToken(Long userId, String refreshToken, String salt);
}