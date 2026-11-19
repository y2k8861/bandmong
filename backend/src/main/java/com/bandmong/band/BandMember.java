package com.bandmong.band;

import com.bandmong.user.Instrument;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "band_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_band_member", columnNames = {"band_id", "user_id"}))
public class BandMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 이 밴드에서 맡은 악기 (자리 하나를 차지한다) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected BandMember() {
    }

    public BandMember(Band band, User user, Instrument instrument, MemberRole role) {
        this.band = band;
        this.user = user;
        this.instrument = instrument;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Band getBand() { return band; }
    public User getUser() { return user; }
    public Instrument getInstrument() { return instrument; }
    public MemberRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
