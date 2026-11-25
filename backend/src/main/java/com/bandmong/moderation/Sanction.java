package com.bandmong.moderation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sanctions", indexes = @Index(name = "idx_sanction_user", columnList = "user_id, ends_at"))
public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SanctionType type;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    /** 정지가 풀리는 시각. 경고는 시작과 같다. */
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /** 관리자가 일찍 풀어 준 시각 */
    @Column(name = "lifted_at")
    private Instant liftedAt;

    protected Sanction() {
    }

    public Sanction(Long userId, SanctionType type, Instant startsAt, Instant endsAt, String reason, Long reportId, Long createdBy) {
        this.userId = userId;
        this.type = type;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
        this.reportId = reportId;
        this.createdBy = createdBy;
    }

    public boolean activeAt(Instant now) {
        return type == SanctionType.SUSPENSION && liftedAt == null && endsAt.isAfter(now);
    }

    public void lift(Instant now) {
        liftedAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public SanctionType getType() { return type; }
    public Instant getEndsAt() { return endsAt; }
    public String getReason() { return reason; }
}
