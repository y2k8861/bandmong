package com.bandmong.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.user.id = :userId and n.id < :before order by n.id desc")
    List<Notification> findPage(@Param("userId") Long userId, @Param("before") Long before, Pageable page);

    @Query("select count(n) from Notification n where n.user.id = :userId and n.readAt is null")
    long countUnread(@Param("userId") Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
