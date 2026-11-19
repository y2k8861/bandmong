package com.bandmong.jam;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** 확정된 합주 한 번 */
@Entity
@Table(name = "jams", indexes = @Index(name = "idx_jams_band_starts", columnList = "band_id, starts_at"))
public class Jam {

    public static final int MIN_MINUTES = 60;
    public static final int MAX_MINUTES = 240;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(length = 40)
    private String place;

    @Column(length = 200)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JamStatus status = JamStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Jam() {
    }

    public Jam(Band band, User createdBy, Instant startsAt, int durationMinutes, String place, String memo) {
        this.band = band;
        this.createdBy = createdBy;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.place = place;
        this.memo = memo;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Instant endsAt() {
        return startsAt.plusSeconds(durationMinutes * 60L);
    }

    /** 끝난 합주 (끝나는 시각 기준) */
    public boolean isOver(Instant now) {
        return !endsAt().isAfter(now);
    }

    public boolean isCanceled() {
        return status == JamStatus.CANCELED;
    }

    /** @return 시작 시각이 바뀌었는지 */
    public boolean reschedule(Instant startsAt, Integer durationMinutes) {
        boolean moved = startsAt != null && !startsAt.equals(this.startsAt);
        if (startsAt != null) this.startsAt = startsAt;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        return moved;
    }

    public void updateNote(String place, String memo) {
        if (place != null) this.place = place.isBlank() ? null : place.trim();
        if (memo != null) this.memo = memo.isBlank() ? null : memo.trim();
    }

    public void cancel() {
        this.status = JamStatus.CANCELED;
    }

    public Long getId() { return id; }
    public Band getBand() { return band; }
    public Instant getStartsAt() { return startsAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getPlace() { return place; }
    public String getMemo() { return memo; }
    public JamStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
}
