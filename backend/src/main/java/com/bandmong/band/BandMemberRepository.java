package com.bandmong.band;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    @Query("select m from BandMember m join fetch m.user where m.band.id = :bandId order by m.joinedAt, m.id")
    List<BandMember> findByBandWithUser(@Param("bandId") Long bandId);

    /** 목록 화면에서 밴드마다 멤버를 따로 조회하지 않도록 한 번에 가져온다 (N+1 방지). */
    @Query("select m from BandMember m join fetch m.user where m.band.id in :bandIds")
    List<BandMember> findByBandIds(@Param("bandIds") Collection<Long> bandIds);

    @Query("select m from BandMember m join fetch m.band b join fetch b.host where m.user.id = :userId order by m.joinedAt desc")
    List<BandMember> findByUserWithBand(@Param("userId") Long userId);

    Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);

    boolean existsByBandIdAndUserId(Long bandId, Long userId);
}
