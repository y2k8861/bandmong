package com.bandmong.dues;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DuesPaymentRepository extends JpaRepository<DuesPayment, Long> {
    List<DuesPayment> findByBandIdAndMonth(Long bandId, String month);
    Optional<DuesPayment> findByBandIdAndUserIdAndMonth(Long bandId, Long userId, String month);
}
