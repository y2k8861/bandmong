package com.bandmong.jam;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * 가능 시간 투표 한 칸. "이 주(월요일 시작)의 이 칸에 나 가능" 한 줄.
 * slot = 요일(0=월…6=일) * 24 + 시작 시(한국 시간).
 */
@Entity
@Table(name = "availabilities",
        uniqueConstraints = @UniqueConstraint(name = "uk_availability",
                columnNames = {"band_id", "user_id", "week_start", "slot"}),
        indexes = @Index(name = "idx_availability_week", columnList = "band_id, week_start"))
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private int slot;

    protected Availability() {
    }

    public Availability(Band band, User user, LocalDate weekStart, int slot) {
        this.band = band;
        this.user = user;
        this.weekStart = weekStart;
        this.slot = slot;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getWeekStart() { return weekStart; }
    public int getSlot() { return slot; }
}
