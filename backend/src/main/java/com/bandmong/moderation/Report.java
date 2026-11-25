package com.bandmong.moderation;

import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status, id"),
        @Index(name = "idx_report_target", columnList = "target_type, target_id")})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 책임질 사람: 신고된 회원 / 메시지를 쓴 사람 / 밴드 방장 (신고 시점 기준) */
    @Column(name = "accused_id", nullable = false)
    private Long accusedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportReason reason;

    @Column(length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** 처리 결과 (예: SUSPEND_7) */
    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "admin_note", length = 300)
    private String adminNote;

    protected Report() {
    }

    public Report(User reporter, ReportTarget targetType, Long targetId, Long accusedId, ReportReason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.accusedId = accusedId;
        this.reason = reason;
        this.detail = detail;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void close(ReportStatus status, String resolution, Long adminId, String note, Instant now) {
        this.status = status;
        this.resolution = resolution;
        this.resolvedBy = adminId;
        this.adminNote = note;
        this.resolvedAt = now;
    }

    public Long getId() { return id; }
    public User getReporter() { return reporter; }
    public ReportTarget getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Long getAccusedId() { return accusedId; }
    public ReportReason getReason() { return reason; }
    public String getDetail() { return detail; }
    public ReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolution() { return resolution; }
    public String getAdminNote() { return adminNote; }
}
