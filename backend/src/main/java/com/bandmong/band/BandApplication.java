package com.bandmong.band;

import com.bandmong.user.Instrument;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** 가입 신청. 기록을 남기기 위해 지우지 않고 상태만 바꾼다. */
@Entity
@Table(name = "band_applications",
        indexes = @Index(name = "idx_app_band_status", columnList = "band_id, status"))
public class BandApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Instrument instrument;

    @Column(length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected BandApplication() {
    }

    public BandApplication(Band band, User user, Instrument instrument, String message) {
        this.band = band;
        this.user = user;
        this.instrument = instrument;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void decide(ApplicationStatus next) {
        this.status = next;
        this.decidedAt = Instant.now();
    }

    public boolean isPending() {
        return status == ApplicationStatus.PENDING;
    }

    public Long getId() { return id; }
    public Band getBand() { return band; }
    public User getUser() { return user; }
    public Instrument getInstrument() { return instrument; }
    public String getMessage() { return message; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
