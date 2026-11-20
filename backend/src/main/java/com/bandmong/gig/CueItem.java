package com.bandmong.gig;

import jakarta.persistence.*;

/** 큐시트 한 줄 */
@Entity
@Table(name = "gig_cue_items")
public class CueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gig_id", nullable = false)
    private Gig gig;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CueType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(name = "duration_sec", nullable = false)
    private int durationSec;

    protected CueItem() {
    }

    public CueItem(CueType type, String title, int durationSec) {
        this.type = type;
        this.title = title;
        this.durationSec = durationSec;
    }

    void attach(Gig gig, int position) {
        this.gig = gig;
        this.position = position;
    }

    public Long getId() { return id; }
    public int getPosition() { return position; }
    public CueType getType() { return type; }
    public String getTitle() { return title; }
    public int getDurationSec() { return durationSec; }
}
