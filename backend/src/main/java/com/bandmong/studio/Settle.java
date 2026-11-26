package com.bandmong.studio;

import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** 합주실비 1인 몫. 예약할 때 그 시점 멤버 수만큼 만든다. */
@Entity
@Table(name = "booking_settles",
        uniqueConstraints = @UniqueConstraint(name = "uk_settle", columnNames = {"booking_id", "user_id"}))
public class Settle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Settle() {
    }

    public Settle(Booking booking, User user, int amount) {
        this.booking = booking;
        this.user = user;
        this.amount = amount;
    }

    public void setPaid(boolean paid, Instant now) {
        paidAt = paid ? (paidAt == null ? now : paidAt) : null;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public User getUser() { return user; }
    public int getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
}
