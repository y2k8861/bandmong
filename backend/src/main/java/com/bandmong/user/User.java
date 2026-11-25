package com.bandmong.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_provider", columnNames = {"provider", "provider_id"}))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** dev / kakao / naver / google */
    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(length = 10)
    private String nickname;

    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Column(length = 30)
    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_instruments", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "instrument", length = 20, nullable = false)
    private Set<Instrument> instruments = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    /** 탈퇴한 시각. 탈퇴하면 개인정보를 지우고 이 계정으로는 다시 로그인할 수 없다. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    public User(String provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    /** 프로필 설정(온보딩)을 마쳤는지. 마치기 전에는 밴드 기능을 막는다. */
    public boolean isOnboarded() {
        return nickname != null && level != null && !instruments.isEmpty();
    }

    public void updateProfile(String nickname, Set<Instrument> instruments, Level level, String region) {
        this.nickname = nickname;
        this.instruments.clear();
        this.instruments.addAll(EnumSet.copyOf(instruments));
        this.level = level;
        this.region = (region == null || region.isBlank()) ? null : region.trim();
    }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isDeleted() { return deletedAt != null; }
    public void promoteToAdmin() { role = Role.ADMIN; }

    /**
     * 탈퇴: 닉네임·이메일·지역·악기를 지우고, 소셜 계정 연결을 끊는다(같은 카카오 계정으로 오면 새 회원).
     * 채팅 기록은 남기되 "탈퇴한 사용자"로 보인다.
     */
    public void withdraw(Instant now) {
        nickname = "탈퇴한 사용자";
        email = null;
        region = null;
        level = null;
        instruments.clear();
        providerId = "deleted-" + id + "-" + now.toEpochMilli();
        role = Role.USER;
        deletedAt = now;
    }

    public Role getRole() { return role; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getId() { return id; }
    public String getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public Level getLevel() { return level; }
    public String getRegion() { return region; }
    public Set<Instrument> getInstruments() { return instruments; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEmail(String email) { this.email = email; }
}
