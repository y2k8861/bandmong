package com.bandmong.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    List<UserBlock> findByBlockerIdOrderByIdDesc(Long blockerId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("select b.blockedId from UserBlock b where b.blockerId = :userId")
    List<Long> blockedBy(@Param("userId") Long userId);

    /** 이 사람을 차단한 사람 중 candidates에 있는 사람 (채팅 방송에서 뺄 사람) */
    @Query("select b.blockerId from UserBlock b where b.blockedId = :senderId and b.blockerId in :candidates")
    List<Long> blockersAmong(@Param("senderId") Long senderId, @Param("candidates") Collection<Long> candidates);
}
