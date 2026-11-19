package com.bandmong.band;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BandRepository extends JpaRepository<Band, Long> {

    /**
     * 자리를 바꾸는 작업(지원·수락·자리 수정)은 밴드 행을 잠그고 한다.
     * 방장이 두 지원자를 거의 동시에 수락해도 마지막 자리를 두 명이 차지하지 못한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Band b where b.id = :id")
    Optional<Band> findByIdForUpdate(@Param("id") Long id);

    @Query("select b from Band b join fetch b.host where b.recruiting = true order by b.createdAt desc, b.id desc")
    List<Band> findRecruiting(Pageable page);
}
