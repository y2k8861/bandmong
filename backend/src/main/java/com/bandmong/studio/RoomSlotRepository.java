package com.bandmong.studio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {

    @Query("select s from RoomSlot s where s.roomId = :roomId and s.slotStart >= :from and s.slotStart < :to")
    List<RoomSlot> findTaken(@Param("roomId") Long roomId, @Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("delete from RoomSlot s where s.bookingId = :bookingId")
    int release(@Param("bookingId") Long bookingId);
}
