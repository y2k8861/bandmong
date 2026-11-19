package com.bandmong.band;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandApplicationRepository extends JpaRepository<BandApplication, Long> {

    Optional<BandApplication> findFirstByBandIdAndUserIdAndStatus(Long bandId, Long userId, ApplicationStatus status);

    @Query("select a from BandApplication a join fetch a.user where a.band.id = :bandId and a.status = :status order by a.createdAt, a.id")
    List<BandApplication> findByBandAndStatus(@Param("bandId") Long bandId, @Param("status") ApplicationStatus status);

    @Query("select a.band.id, count(a) from BandApplication a where a.band.id in :bandIds and a.status = :status group by a.band.id")
    List<Object[]> countByBandAndStatus(@Param("bandIds") Collection<Long> bandIds, @Param("status") ApplicationStatus status);

    @Query("select a from BandApplication a join fetch a.band b join fetch b.host where a.user.id = :userId and a.status = :status order by a.createdAt desc")
    List<BandApplication> findByUserAndStatus(@Param("userId") Long userId, @Param("status") ApplicationStatus status);
}
