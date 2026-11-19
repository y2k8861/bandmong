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
