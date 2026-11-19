package com.bandmong.jam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    @Query("select a from Availability a where a.band.id = :bandId and a.weekStart = :week")
    List<Availability> findWeek(@Param("bandId") Long bandId, @Param("week") LocalDate week);
}
