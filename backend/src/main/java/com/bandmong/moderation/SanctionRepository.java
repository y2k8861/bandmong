package com.bandmong.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    @Query("select s from Sanction s where s.userId = :userId and s.type = com.bandmong.moderation.SanctionType.SUSPENSION and s.liftedAt is null and s.endsAt > :now order by s.endsAt desc")
    List<Sanction> findActive(@Param("userId") Long userId, @Param("now") Instant now);

    List<Sanction> findByUserIdOrderByIdDesc(Long userId);
}
