package com.bandmong.studio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudioRepository extends JpaRepository<Studio, Long> {
    @Query("select distinct s from Studio s left join fetch s.rooms order by s.id")
    List<Studio> findAllWithRooms();
}
