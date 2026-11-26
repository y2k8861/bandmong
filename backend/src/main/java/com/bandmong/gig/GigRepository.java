package com.bandmong.gig;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GigRepository extends JpaRepository<Gig, Long> {
    @Query("select g from Gig g where g.band.id = :bandId and g.startsAt > :since order by g.startsAt")
    List<Gig> findForBand(@Param("bandId") Long bandId, @Param("since") Instant since);

    /**
     * 큐시트 저장용: 공연 행 자체가 안 바뀌어도(줄만 바뀜) 커밋 때 버전을 반드시 올린다.
     * UPDATE … SET version=v+1 WHERE version=v 로, 동시에 저장한 두 번째 사람은 충돌이 난다.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select g from Gig g where g.id = :id")
    Optional<Gig> findForCueUpdate(@Param("id") Long id);
}
