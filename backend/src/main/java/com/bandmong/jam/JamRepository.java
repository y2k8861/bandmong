package com.bandmong.jam;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JamRepository extends JpaRepository<Jam, Long> {

    /** 참석 응답처럼 한 합주에 쓰는 작업은 합주 행을 잠그고 한다 (같은 사람 두 번 탭 → 중복 행 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jam j where j.id = :id")
    Optional<Jam> findByIdForUpdate(@Param("id") Long id);

    /** 아직 끝나지 않은 합주 (취소 포함 — 멤버가 취소 소식도 봐야 하므로) */
    @Query("select j from Jam j where j.band.id = :bandId and j.startsAt > :since order by j.startsAt, j.id")
    List<Jam> findUpcoming(@Param("bandId") Long bandId, @Param("since") Instant since);

    @Query("select j from Jam j where j.band.id = :bandId and j.startsAt <= :since and j.status = com.bandmong.jam.JamStatus.SCHEDULED order by j.startsAt desc, j.id desc")
    List<Jam> findPast(@Param("bandId") Long bandId, @Param("since") Instant since, Pageable page);

    /** 여러 밴드의 예정 합주를 한 번에 (내 밴드 카드의 "다음 합주") */
    @Query("select j from Jam j where j.band.id in :bandIds and j.startsAt > :since and j.status = com.bandmong.jam.JamStatus.SCHEDULED order by j.startsAt, j.id")
    List<Jam> findUpcomingForBands(@Param("bandIds") Collection<Long> bandIds, @Param("since") Instant since);
}
