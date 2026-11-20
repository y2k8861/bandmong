package com.bandmong.studio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettleRepository extends JpaRepository<Settle, Long> {

    @Query("select s from Settle s join fetch s.user where s.booking.id in :ids order by s.id")
    List<Settle> findForBookings(@Param("ids") Collection<Long> ids);

    Optional<Settle> findByBookingIdAndUserId(Long bookingId, Long userId);
}
