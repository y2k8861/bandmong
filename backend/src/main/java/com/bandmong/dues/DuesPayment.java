package com.bandmong.dues;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** 이 달 회비를 냈다는 기록. 안 낸 사람은 행이 없다. */
@Entity
@Table(name = "dues_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_dues", columnNames = {"band_id", "user_id", "due_month"}))
public class DuesPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** YYYY-MM. 컬럼 이름이 month면 H2·일부 DB 예약어와 부딪혀서 due_month로 둔다. */
    @Column(name = "due_month", nullable = false, length = 7)
    private String month;

    /** 그때 회비 금액 (나중에 회비가 바뀌어도 그 달 기록은 그대로) */
    @Column(nullable = false)
    private int amount;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    /** 표시한 사람 (본인 또는 방장) */
    @Column(name = "marked_by", nullable = false)
    private Long markedBy;

    protected DuesPayment() {
    }

    public DuesPayment(Band band, User user, String month, int amount, Long markedBy) {
        this.band = band;
        this.user = user;
        this.month = month;
        this.amount = amount;
        this.markedBy = markedBy;
        this.paidAt = Instant.now();
    }

    public User getUser() { return user; }
    public String getMonth() { return month; }
    public int getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
    public Long getMarkedBy() { return markedBy; }
}
