package com.bandmong.app;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 소셜 로그인을 마친 브라우저가 앱으로 돌아갈 때 쥐여 주는 일회용 코드 (2분, 한 번만).
 * 토큰을 주소창(bandmong://…)에 직접 실어 보내지 않으려고 코드로 한 번 바꿔 받는다.
 */
@Entity
@Table(name = "app_login_codes",
        uniqueConstraints = @UniqueConstraint(name = "uk_login_code_hash", columnNames = "code_hash"))
public class LoginCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected LoginCode() {
    }

    public LoginCode(String codeHash, Long userId, Instant expiresAt) {
        this.codeHash = codeHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public boolean usableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void use(Instant now) {
        usedAt = now;
    }

    public Long getUserId() { return userId; }
}
