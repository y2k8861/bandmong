package com.bandmong.app;

import jakarta.persistence.*;

import java.time.Instant;

/** 푸시 알림을 받을 기기 (FCM 등록 토큰). 한 사람이 기기 여러 대를 쓸 수 있다. */
@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_token", columnNames = "token"),
        indexes = @Index(name = "idx_device_user", columnList = "user_id"))
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {
    }

    public DeviceToken(Long userId, String token, DevicePlatform platform, Instant now) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.updatedAt = now;
    }

    /** 같은 기기에 다른 사람이 로그인하면 주인을 바꾼다 */
    public void reassign(Long userId, DevicePlatform platform, Instant now) {
        this.userId = userId;
        this.platform = platform;
        this.updatedAt = now;
    }

    public Long getUserId() { return userId; }
    public String getToken() { return token; }
    public DevicePlatform getPlatform() { return platform; }
}
