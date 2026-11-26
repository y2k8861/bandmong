package com.bandmong.app;

import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 앱 로그인 토큰. 원문은 앱에만 있고, 서버에는 SHA-256 해시만 저장한다
 * (DB가 새어 나가도 토큰으로 로그인할 수 없게).
 */
@Entity
@Table(name = "app_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "idx_app_token_user", columnList = "user_id"))
public class AppToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** 예: "iPhone" — 마이 화면에서 로그인 기기 목록을 보여 줄 때 */
    @Column(name = "device_name", length = 60)
    private String deviceName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AppToken() {
    }

    public AppToken(User user, String tokenHash, String deviceName, Instant now, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.deviceName = deviceName;
        this.createdAt = now;
        this.lastUsedAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean usableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = now;
    }

    public void touch(Instant now) {
        lastUsedAt = now;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
