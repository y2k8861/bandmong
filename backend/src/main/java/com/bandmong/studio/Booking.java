package com.bandmong.studio;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bookings", indexes = @Index(name = "idx_booking_band", columnList = "band_id, starts_at"))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    /** 어느 합주를 위한 예약인지 (선택) */
    @Column(name = "jam_id")
    private Long jamId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private int hours;

    /** 예약할 때 계산한 금액. 나중에 합주실 가격이 바뀌어도 이 값은 그대로. */
    @Column(nullable = false)
    private int price;

    @Column(name = "per_person", nullable = false)
    private int perPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booked_by", nullable = false)
    private User bookedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Booking() {
    }

    public Booking(Room room, Band band, Long jamId, Instant startsAt, int hours, int price, int perPerson, User bookedBy) {
        this.room = room;
        this.band = band;
        this.jamId = jamId;
        this.startsAt = startsAt;
        this.hours = hours;
        this.price = price;
        this.perPerson = perPerson;
        this.bookedBy = bookedBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Instant endsAt() {
        return startsAt.plusSeconds(hours * 3600L);
    }

    public void cancel() {
        status = BookingStatus.CANCELED;
    }

    public boolean isCanceled() {
        return status == BookingStatus.CANCELED;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Band getBand() { return band; }
    public Long getJamId() { return jamId; }
    public Instant getStartsAt() { return startsAt; }
    public int getHours() { return hours; }
    public int getPrice() { return price; }
    public int getPerPerson() { return perPerson; }
    public BookingStatus getStatus() { return status; }
    public User getBookedBy() { return bookedBy; }
}
