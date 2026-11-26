package com.bandmong.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AppTokenRepository extends JpaRepository<AppToken, Long> {

    @Query("select t from AppToken t join fetch t.user where t.tokenHash = :hash")
    Optional<AppToken> findByHash(@Param("hash") String hash);

    @Modifying
    @Query("update AppToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") Instant now);
}
