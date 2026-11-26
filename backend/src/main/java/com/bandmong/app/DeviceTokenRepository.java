package com.bandmong.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(Long userId);

    @Modifying
    @Query("delete from DeviceToken d where d.userId = :userId")
    int deleteAllOf(@Param("userId") Long userId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("delete from DeviceToken d where d.token = :token")
    int deleteToken(@Param("token") String token);
}
