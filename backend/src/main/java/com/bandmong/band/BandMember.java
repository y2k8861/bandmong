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

    /** 채팅을 어디까지 읽었는지 (메시지 id). 이보다 큰 id가 안 읽은 메시지. */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

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

    /** 뒤로 가지는 않는다 (다른 기기에서 늦게 도착한 읽음 표시가 앞선 기록을 덮지 않게) */
    public void markRead(long messageId) {
        if (lastReadMessageId == null || messageId > lastReadMessageId) lastReadMessageId = messageId;
    }

    public void changeRole(MemberRole role) { this.role = role; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public Long getId() { return id; }
    public Band getBand() { return band; }
    public User getUser() { return user; }
    public Instrument getInstrument() { return instrument; }
    public MemberRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
