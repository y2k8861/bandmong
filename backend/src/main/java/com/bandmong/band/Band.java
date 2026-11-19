package com.bandmong.band;

import com.bandmong.user.Instrument;
import com.bandmong.user.Level;
import com.bandmong.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(name = "bands", indexes = @Index(name = "idx_bands_recruiting", columnList = "recruiting, created_at"))
public class Band {

    public static final int MAX_SEATS = 8;
    public static final int MAX_PER_INSTRUMENT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;

    /** 원하는 실력. null이면 상관없음. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Column(nullable = false, length = 20)
    private String area;

    /** 정기 모임 (예: 매주 금 20:00). 실제 일정은 3단계 합주 일정에서 다룬다. */
    @Column(length = 20)
    private String schedule;

    @Column(length = 300)
    private String intro;

    @Column(nullable = false)
    private boolean recruiting = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    /** 악기별 전체 자리 수 (방장 자리 포함). 빈자리 = 자리 수 - 그 악기 멤버 수. */
    // 목록에서 밴드 100개의 자리를 한 번에 몇 개씩 묶어 읽는다 (밴드마다 따로 조회하는 N+1 방지)
    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "band_seats", joinColumns = @JoinColumn(name = "band_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "instrument", length = 20)
    @Column(name = "seat_count", nullable = false)
    private Map<Instrument, Integer> seats = new EnumMap<>(Instrument.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Band() {
    }

    public Band(User host, String title, Genre genre, Level level, String area, String schedule, String intro,
                Map<Instrument, Integer> seats) {
        this.host = host;
        this.title = title;
        this.genre = genre;
        this.level = level;
        this.area = area;
        this.schedule = schedule;
        this.intro = intro;
        replaceSeats(seats);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void replaceSeats(Map<Instrument, Integer> next) {
        seats.clear();
        next.forEach((k, v) -> {
            if (v != null && v > 0) seats.put(k, v);
        });
    }

    public int seatCount(Instrument instrument) {
        return seats.getOrDefault(instrument, 0);
    }

    public int totalSeats() {
        return seats.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isHost(Long userId) {
        return host.getId().equals(userId);
    }

    public void updateInfo(String intro, String schedule, Boolean recruiting) {
        if (intro != null) this.intro = intro.isBlank() ? null : intro.trim();
        if (schedule != null) this.schedule = schedule.isBlank() ? null : schedule.trim();
        if (recruiting != null) this.recruiting = recruiting;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Genre getGenre() { return genre; }
    public Level getLevel() { return level; }
    public String getArea() { return area; }
    public String getSchedule() { return schedule; }
    public String getIntro() { return intro; }
    public boolean isRecruiting() { return recruiting; }
    public User getHost() { return host; }
    public Map<Instrument, Integer> getSeats() { return seats; }
    public Instant getCreatedAt() { return createdAt; }
}
