package com.bandmong.gig;

import com.bandmong.activity.Events;
import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.gig.dto.GigRequests;
import com.bandmong.gig.dto.GigView;
import com.bandmong.web.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GigService {

    public static final String STALE = "다른 멤버가 먼저 큐시트를 고쳤어요. 최신으로 불러온 뒤 다시 고쳐 주세요";

    /** 처음 만들 때 넣어 주는 준비 목록 */
    static final List<String> DEFAULT_CHECKS = List.of(
            "셋리스트 확정", "공연 전 리허설 합주 잡기", "케이블·스트랩·여분 줄 챙기기",
            "공연장 무대 크기·장비 확인", "홍보 포스터 공유");

    private final GigRepository gigs;
    private final GigCheckRepository checks;
    private final BandRepository bands;
    private final BandMemberRepository members;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public GigService(GigRepository gigs, GigCheckRepository checks, BandRepository bands,
                      BandMemberRepository members, ApplicationEventPublisher events, Clock clock) {
        this.gigs = gigs;
        this.checks = checks;
        this.bands = bands;
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    public GigView create(Long userId, Long bandId, GigRequests.Create r) {
        Band band = bands.findById(bandId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요"));
        if (!band.isHost(userId)) throw forbidden("방장만 공연을 잡을 수 있어요");
        List<BandMember> list = members.findByBandWithUser(bandId);
        BandMember me = list.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst().orElseThrow();

        Gig gig = new Gig(band, me.getUser(), r.title().trim(), validStart(r.startsAt()),
                r.venue() == null || r.venue().isBlank() ? null : r.venue().trim(), r.slotMinutes(), clock.instant());
        // 큐시트 초안: 여는 인사와 마무리 인사. 곡은 멤버들이 채운다.
        gig.replaceItems(new java.util.ArrayList<>(List.of(
                new CueItem(CueType.MC, "여는 인사·밴드 소개", 60),
                new CueItem(CueType.MC, "마무리 인사", 45))), clock.instant());
        gigs.save(gig);
        DEFAULT_CHECKS.forEach(t -> gig.getChecks().add(new GigCheck(gig, t)));
        gigs.flush();
        events.publishEvent(new Events.GigScheduled(gig.getId(), userId));
        return view(gig, userId, list);
    }

    @Transactional(readOnly = true)
    public List<GigView> list(Long userId, Long bandId) {
        List<BandMember> list = requireMember(bandId, userId);
        return gigs.findForBand(bandId, clock.instant().minus(Duration.ofDays(1))).stream()
                .map(g -> view(g, userId, list)).toList();
    }

    @Transactional(readOnly = true)
    public GigView detail(Long userId, Long gigId) {
        Gig g = gig(gigId);
        return view(g, userId, requireMember(g.getBand().getId(), userId));
    }

    public GigView update(Long userId, Long gigId, GigRequests.Update r) {
        Gig g = gig(gigId);
        if (!g.getBand().isHost(userId)) throw forbidden("방장만 공연 정보를 바꿀 수 있어요");
        requireOpen(g);
        g.update(r.title() == null ? null : r.title().trim(), r.startsAt() == null ? null : validStart(r.startsAt()),
                r.venue(), r.slotMinutes(), clock.instant());
        gigs.flush();
        return view(g, userId, members.findByBandWithUser(g.getBand().getId()));
    }

    public GigView cancel(Long userId, Long gigId) {
        Gig g = gig(gigId);
        if (!g.getBand().isHost(userId)) throw forbidden("방장만 공연을 취소할 수 있어요");
        requireOpen(g);
        g.cancel(clock.instant());
        gigs.flush();
        return view(g, userId, members.findByBandWithUser(g.getBand().getId()));
    }

    /**
     * 큐시트 저장 (멤버 누구나). 내가 본 버전이 지금 버전과 다르면 409.
     * 확인과 저장 사이에 다른 저장이 끼어들면 @Version이 UPDATE … WHERE version=? 로 한 번 더 막는다.
     */
    public GigView saveItems(Long userId, Long gigId, GigRequests.Items r) {
        Gig g = gig(gigId);
        List<BandMember> list = requireMember(g.getBand().getId(), userId);
        requireOpen(g);
        if (g.getVersion() != r.version()) throw new ApiException(HttpStatus.CONFLICT, STALE);
        g.replaceItems(r.items().stream().map(i -> new CueItem(i.type(), i.title().trim(), i.durationSec()))
                .collect(Collectors.toList()), clock.instant());
        gigs.flush();
        return view(g, userId, list);
    }

    public GigView addCheck(Long userId, Long gigId, String text) {
        Gig g = gig(gigId);
        List<BandMember> list = requireMember(g.getBand().getId(), userId);
        requireOpen(g);
        if (g.getChecks().size() >= 30) throw new ApiException(HttpStatus.BAD_REQUEST, "체크리스트는 30개까지예요");
        g.getChecks().add(checks.save(new GigCheck(g, text.trim())));
        return view(g, userId, list);
    }

    public GigView markCheck(Long userId, Long gigId, Long checkId, boolean done) {
        Gig g = gig(gigId);
        List<BandMember> list = requireMember(g.getBand().getId(), userId);
        check(g, checkId).mark(done, userId);
        checks.flush();
        return view(g, userId, list);
    }

    public GigView deleteCheck(Long userId, Long gigId, Long checkId) {
        Gig g = gig(gigId);
        List<BandMember> list = requireMember(g.getBand().getId(), userId);
        g.getChecks().remove(check(g, checkId));
        checks.flush();
        return view(g, userId, list);
    }

    // ───────── 도우미 ─────────

    private GigView view(Gig g, Long userId, List<BandMember> list) {
        Map<Long, String> names = list.stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m.getUser().getNickname()));
        List<GigView.Item> items = g.getItems().stream()
                .map(i -> new GigView.Item(i.getId(), i.getType(), i.getTitle(), i.getDurationSec())).toList();
        List<GigView.Check> cs = g.getChecks().stream()
                .map(c -> new GigView.Check(c.getId(), c.getText(), c.isDone(),
                        c.getDoneBy() == null ? null : names.getOrDefault(c.getDoneBy(), "나간 멤버")))
                .toList();
        return new GigView(g.getId(), g.getBand().getId(), g.getBand().getTitle(), g.getTitle(), g.getStartsAt(),
                g.getVenue(), g.getSlotMinutes(), g.getStatus(), g.getVersion(), g.getBand().isHost(userId),
                g.totalSeconds(), items, cs, (int) cs.stream().filter(GigView.Check::done).count());
    }

    private Instant validStart(OffsetDateTime at) {
        Instant t = at.toInstant(), now = clock.instant();
        if (!t.isAfter(now)) throw new ApiException(HttpStatus.BAD_REQUEST, "지난 날짜로는 잡을 수 없어요");
        if (t.isAfter(now.plus(Duration.ofDays(365)))) throw new ApiException(HttpStatus.BAD_REQUEST, "1년 안으로 잡아 주세요");
        return t;
    }

    private void requireOpen(Gig g) {
        if (g.getStatus() == GigStatus.CANCELED) throw new ApiException(HttpStatus.CONFLICT, "취소된 공연이에요");
    }

    private Gig gig(Long id) {
        return gigs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없어요"));
    }

    private GigCheck check(Gig g, Long checkId) {
        return g.getChecks().stream().filter(c -> c.getId().equals(checkId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "체크 항목을 찾을 수 없어요"));
    }

    private List<BandMember> requireMember(Long bandId, Long userId) {
        List<BandMember> list = members.findByBandWithUser(bandId);
        if (list.stream().noneMatch(m -> m.getUser().getId().equals(userId))) throw forbidden("밴드 멤버만 볼 수 있어요");
        return list;
    }

    private static ApiException forbidden(String m) {
        return new ApiException(HttpStatus.FORBIDDEN, m);
    }
}
