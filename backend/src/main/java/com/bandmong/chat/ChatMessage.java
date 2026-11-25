package com.bandmong.chat;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages",
        indexes = @Index(name = "idx_chat_band_id", columnList = "band_id, id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_client",
                columnNames = {"band_id", "sender_id", "client_id"}))
public class ChatMessage {

    public static final int MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    /** 시스템 메시지는 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type;

    @Column(nullable = false, length = MAX_LENGTH)
    private String body;

    /** 보낸 기기가 붙인 id. 응답을 못 받고 다시 보내도 한 번만 저장된다. */
    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 신고 처리로 가려짐. 기록은 남기고 내용만 보이지 않는다. */
    @Column(nullable = false)
    private boolean hidden;

    protected ChatMessage() {
    }

    public ChatMessage(Band band, User sender, MessageType type, String body, String clientId) {
        this.band = band;
        this.sender = sender;
        this.type = type;
        this.body = body;
        this.clientId = clientId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void hide() { hidden = true; }
    public boolean isHidden() { return hidden; }
    public Long getId() { return id; }
    public Band getBand() { return band; }
    public User getSender() { return sender; }
    public MessageType getType() { return type; }
    public String getBody() { return body; }
    public String getClientId() { return clientId; }
    public Instant getCreatedAt() { return createdAt; }
}
