package com.bandmong.jam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JamRsvpRepository extends JpaRepository<JamRsvp, Long> {

    Optional<JamRsvp> findByJamIdAndUserId(Long jamId, Long userId);

    @Query("select r from JamRsvp r where r.jam.id in :jamIds")
    List<JamRsvp> findByJamIds(@Param("jamIds") Collection<Long> jamIds);

    @Query("select r from JamRsvp r where r.jam.id in :jamIds and r.user.id = :userId")
    List<JamRsvp> findMine(@Param("jamIds") Collection<Long> jamIds, @Param("userId") Long userId);

    @Modifying
    @Query("delete from JamRsvp r where r.jam.id = :jamId")
    int deleteByJam(@Param("jamId") Long jamId);
}
