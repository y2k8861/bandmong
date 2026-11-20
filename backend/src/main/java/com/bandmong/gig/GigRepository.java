package com.bandmong.gig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface GigRepository extends JpaRepository<Gig, Long> {
    @Query("select g from Gig g where g.band.id = :bandId and g.startsAt > :since order by g.startsAt")
    List<Gig> findForBand(@Param("bandId") Long bandId, @Param("since") Instant since);
}
