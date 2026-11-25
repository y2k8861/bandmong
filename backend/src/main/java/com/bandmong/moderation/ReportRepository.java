package com.bandmong.moderation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTarget type, Long targetId, ReportStatus status);

    @Query("select count(r) from Report r where r.reporter.id = :userId and r.createdAt > :since")
    long countSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("select r from Report r join fetch r.reporter where r.status = :status order by r.id desc")
    List<Report> findByStatus(@Param("status") ReportStatus status, Pageable page);

    List<Report> findByTargetTypeAndTargetIdAndStatus(ReportTarget type, Long targetId, ReportStatus status);

    @Query("select count(r) from Report r where r.accusedId = :userId")
    long countAgainst(@Param("userId") Long userId);
}
