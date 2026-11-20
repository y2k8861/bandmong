package com.bandmong.gig;

import com.bandmong.band.Band;
import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gigs", indexes = @Index(name = "idx_gig_band", columnList = "band_id, starts_at"))
public class Gig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 낙관적 잠금. 큐시트를 저장할 때 "내가 본 버전"을 같이 보내고, 그 사이 누가 고쳤으면 409.
     * 멤버 여럿이 같은 큐시트를 고치는데, 나중 저장이 앞사람 수정을 말없이 덮어쓰지 않게 한다.
     */
    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(length = 40)
    private String venue;

    /** 우리 밴드에게 주어진 공연 시간(분) */
    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GigStatus status = GigStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "gig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<CueItem> items = new ArrayList<>();

    // 체크리스트는 큐시트 버전과 무관하게 따로 고친다 (체크 하나 했다고 큐시트 충돌이 나면 안 됨)
    @OneToMany(mappedBy = "gig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @org.hibernate.annotations.OptimisticLock(excluded = true)
    private List<GigCheck> checks = new ArrayList<>();

    protected Gig() {
    }

    public Gig(Band band, User createdBy, String title, Instant startsAt, String venue, int slotMinutes) {
        this.band = band;
        this.createdBy = createdBy;
        this.title = title;
        this.startsAt = startsAt;
        this.venue = venue;
        this.slotMinutes = slotMinutes;
        this.updatedAt = Instant.now();
    }

    /** 큐시트 통째로 바꾸기. updatedAt을 바꿔 버전이 확실히 오르게 한다. */
    public void replaceItems(List<CueItem> next) {
        items.clear();
        for (int i = 0; i < next.size(); i++) {
            CueItem it = next.get(i);
            it.attach(this, i);
            items.add(it);
        }
        updatedAt = Instant.now();
    }


    public void update(String title, Instant startsAt, String venue, Integer slotMinutes) {
        if (title != null) this.title = title;
        if (startsAt != null) this.startsAt = startsAt;
        if (venue != null) this.venue = venue.isBlank() ? null : venue.trim();
        if (slotMinutes != null) this.slotMinutes = slotMinutes;
        updatedAt = Instant.now();
    }

    public void cancel() {
        status = GigStatus.CANCELED;
        updatedAt = Instant.now();
    }

    public int totalSeconds() {
        return items.stream().mapToInt(CueItem::getDurationSec).sum();
    }

    public Long getId() { return id; }
    public long getVersion() { return version; }
    public Band getBand() { return band; }
    public String getTitle() { return title; }
    public Instant getStartsAt() { return startsAt; }
    public String getVenue() { return venue; }
    public int getSlotMinutes() { return slotMinutes; }
    public GigStatus getStatus() { return status; }
    public List<CueItem> getItems() { return items; }
    public List<GigCheck> getChecks() { return checks; }
}
