package com.bandmong.notification;

import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_noti_user_id", columnList = "user_id, id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(length = 200)
    private String body;

    /** 누르면 갈 화면 (예: /jams/5) */
    @Column(length = 100)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(User user, NotificationType type, String title, String body, String link) {
        this.user = user;
        this.type = type;
        this.title = cut(title, 60);
        this.body = cut(body, 200);
        this.link = link;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void markRead() {
        if (readAt == null) readAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
