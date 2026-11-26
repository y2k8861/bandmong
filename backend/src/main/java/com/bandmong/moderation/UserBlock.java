package com.bandmong.moderation;

import jakarta.persistence.*;

import java.time.Instant;

/** blocker가 blocked를 차단. 차단한 사람에게만 영향이 있고, 상대에게는 알리지 않는다. */
@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_block", columnNames = {"blocker_id", "blocked_id"}),
        indexes = @Index(name = "idx_block_blocked", columnList = "blocked_id"))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = Instant.now();
    }

    public Long getBlockerId() { return blockerId; }
    public Long getBlockedId() { return blockedId; }
    public Instant getCreatedAt() { return createdAt; }
}
