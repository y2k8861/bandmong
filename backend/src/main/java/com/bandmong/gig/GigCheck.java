package com.bandmong.gig;

import jakarta.persistence.*;

/** 공연 준비 체크리스트 한 줄. 한 줄씩 따로 체크하므로 큐시트 버전과 상관없다. */
@Entity
@Table(name = "gig_checks")
public class GigCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gig_id", nullable = false)
    private Gig gig;

    @Column(nullable = false, length = 60)
    private String text;

    @Column(nullable = false)
    private boolean done;

    /** 체크한 사람 */
    @Column(name = "done_by")
    private Long doneBy;

    protected GigCheck() {
    }

    public GigCheck(Gig gig, String text) {
        this.gig = gig;
        this.text = text;
    }

    public void mark(boolean done, Long userId) {
        this.done = done;
        this.doneBy = done ? userId : null;
    }

    public Long getId() { return id; }
    public Gig getGig() { return gig; }
    public String getText() { return text; }
    public boolean isDone() { return done; }
    public Long getDoneBy() { return doneBy; }
}
