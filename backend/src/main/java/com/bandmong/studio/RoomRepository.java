package com.bandmong.studio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("select r from Room r join fetch r.studio where r.id = :id")
    Optional<Room> findWithStudio(@Param("id") Long id);
}
