package com.bandmong.band;

import com.bandmong.band.dto.*;
import com.bandmong.jam.JamService;
import com.bandmong.jam.dto.JamBrief;
import com.bandmong.user.Instrument;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.bandmong.band.ApplicationStatus.*;

@Service
@Transactional
public class BandService {

    /** 찾기 목록은 최신 모집 100개까지. 사용자가 늘면 커서 기반 페이지로 바꾼다. */
    private static final int LIST_LIMIT = 100;

    private final BandRepository bands;
    private final BandMemberRepository members;
    private final BandApplicationRepository applications;
    private final UserRepository users;
    private final JamService jams;

    public BandService(BandRepository bands, BandMemberRepository members,
                       BandApplicationRepository applications, UserRepository users, JamService jams) {
        this.bands = bands;
        this.members = members;
        this.applications = applications;
        this.users = users;
        this.jams = jams;
    }

    // ───────── 만들기 · 설정 ─────────

    public BandDetail create(Long userId, BandCreateRequest r) {
        User me = onboarded(userId);
        if (!me.getInstruments().contains(r.hostInstrument())) {
            throw bad("내 프로필에 있는 악기로 골라 주세요");
        }
        Map<Instrument, Integer> seats = new EnumMap<>(Instrument.class);
        r.seats().forEach((k, v) -> seats.put(k, v == null ? 0 : v));
        seats.merge(r.hostInstrument(), 1, Math::max); // 방장 자리는 반드시 포함
        validateSeats(seats, Map.of());
        if (seats.values().stream().mapToInt(Integer::intValue).sum() < 2) {
            throw bad("함께할 자리를 하나 이상 만들어 주세요");
        }

        Band band = bands.save(new Band(me, r.title().trim(), r.genre(), r.level(), r.area().trim(),
                blankToNull(r.schedule()), blankToNull(r.intro()), seats));
        members.save(new BandMember(band, me, r.hostInstrument(), MemberRole.HOST));
        return detail(userId, band.getId());
    }

    public BandDetail update(Long userId, Long bandId, BandUpdateRequest r) {
        Band band = lockBand(bandId);
        requireHost(band, userId);
        if (r.seats() != null) {
            Map<Instrument, Integer> seats = new EnumMap<>(Instrument.class);
            r.seats().forEach((k, v) -> seats.put(k, v == null ? 0 : v));
            validateSeats(seats, filledCounts(members.findByBandWithUser(bandId)));
            band.replaceSeats(seats);
        }
        band.updateInfo(r.intro(), r.schedule(), r.recruiting());
        return detail(userId, bandId);
    }

    // ───────── 조회 ─────────

    @Transactional(readOnly = true)
    public List<BandSummary> explore(Long userId, Genre genre, Instrument instrument, String area) {
        List<Band> list = bands.findRecruiting(PageRequest.of(0, LIST_LIMIT));
        Map<Long, List<BandMember>> byBand = membersByBand(list);
        Set<Long> pending = applications.findByUserAndStatus(userId, PENDING).stream()
                .map(a -> a.getBand().getId()).collect(Collectors.toSet());
        String q = area == null ? "" : area.trim();

        return list.stream()
                .filter(b -> genre == null || b.getGenre() == genre)
                .filter(b -> q.isEmpty() || b.getArea().contains(q))
                .map(b -> summary(b, byBand.getOrDefault(b.getId(), List.of()), userId,
                        pending.contains(b.getId()), null, null))
                .filter(s -> s.seats().stream().anyMatch(seat -> seat.open() > 0))
                .filter(s -> instrument == null || s.seats().stream()
                        .anyMatch(seat -> seat.instrument() == instrument && seat.open() > 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyBandsResponse myBands(Long userId) {
        List<Band> mine = members.findByUserWithBand(userId).stream().map(BandMember::getBand).toList();
        Map<Long, List<BandMember>> byBand = membersByBand(mine);
        List<Long> hostIds = mine.stream().filter(b -> b.isHost(userId)).map(Band::getId).toList();
        Map<Long, Long> pendingCounts = hostIds.isEmpty() ? Map.of()
                : applications.countByBandAndStatus(hostIds, PENDING).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        Map<Long, JamBrief> next = jams.nextJams(mine.stream().map(Band::getId).toList(), userId);
        List<BandSummary> bandsOut = mine.stream()
                .map(b -> summary(b, byBand.get(b.getId()), userId, false,
                        b.isHost(userId) ? pendingCounts.getOrDefault(b.getId(), 0L).intValue() : null,
                        next.get(b.getId())))
                // 다음 합주가 가까운 밴드부터, 없으면 최근 가입 순
                .sorted(Comparator.comparing((BandSummary x) -> x.nextJam() == null ? java.time.Instant.MAX : x.nextJam().startsAt()))
                .toList();

        List<Band> applied = applications.findByUserAndStatus(userId, PENDING).stream()
                .map(BandApplication::getBand).toList();
        Map<Long, List<BandMember>> appliedMembers = membersByBand(applied);
        List<BandSummary> appliedOut = applied.stream()
                .map(b -> summary(b, appliedMembers.getOrDefault(b.getId(), List.of()), userId, true, null, null))
                .toList();
        return new MyBandsResponse(bandsOut, appliedOut);
    }

    @Transactional(readOnly = true)
    public BandDetail detail(Long userId, Long bandId) {
        Band band = bands.findById(bandId).orElseThrow(BandService::notFound);
        List<BandMember> list = members.findByBandWithUser(bandId);
        Optional<BandApplication> mine = applications.findFirstByBandIdAndUserIdAndStatus(bandId, userId, PENDING);
        MyRelation rel = relation(list, userId, mine.isPresent());
        List<ApplicationView> waiting = rel == MyRelation.HOST
                ? applications.findByBandAndStatus(bandId, PENDING).stream().map(ApplicationView::of).toList()
                : null;

        JamBrief next = (rel == MyRelation.HOST || rel == MyRelation.MEMBER)
                ? jams.nextJams(List.of(bandId), userId).get(bandId) : null;

        return new BandDetail(band.getId(), band.getTitle(), band.getGenre(), band.getLevel(), band.getArea(),
                band.getSchedule(), band.getIntro(), band.isRecruiting(), band.getCreatedAt(),
                seatViews(band, list), list.stream().map(MemberView::of).toList(), rel,
                mine.map(ApplicationView::of).orElse(null), waiting, next);
    }

    // ───────── 지원 · 수락 · 거절 ─────────

    public BandDetail apply(Long userId, Long bandId, ApplyRequest r) {
        User me = onboarded(userId);
        Band band = lockBand(bandId);
        List<BandMember> list = members.findByBandWithUser(bandId);
        if (list.stream().anyMatch(m -> m.getUser().getId().equals(userId))) {
            throw conflict("이미 이 밴드 멤버예요");
        }
        if (applications.findFirstByBandIdAndUserIdAndStatus(bandId, userId, PENDING).isPresent()) {
            throw conflict("이미 지원했어요. 방장의 답을 기다려 주세요");
        }
        if (!band.isRecruiting()) {
            throw conflict("지금은 멤버를 모집하지 않아요");
        }
        if (!me.getInstruments().contains(r.instrument())) {
            throw bad("내 프로필에 있는 악기로 지원해 주세요");
        }
        if (openSeats(band, list, r.instrument()) <= 0) {
            throw conflict(r.instrument().label() + " 자리는 지금 비어 있지 않아요");
        }
        applications.save(new BandApplication(band, me, r.instrument(), blankToNull(r.message())));
        return detail(userId, bandId);
    }

    public BandDetail cancel(Long userId, Long bandId) {
        BandApplication app = applications.findFirstByBandIdAndUserIdAndStatus(bandId, userId, PENDING)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "대기 중인 지원이 없어요"));
        app.decide(CANCELED);
        return detail(userId, bandId);
    }

    /**
     * 수락: 밴드 행을 먼저 잠근 뒤 지원서와 빈자리를 다시 읽는다.
     * 잠금 → 확인 → 저장 순서라서, 마지막 한 자리에 동시 수락이 들어와도 한 명만 들어간다.
     */
    public BandDetail accept(Long userId, Long bandId, Long applicationId) {
        Band band = lockBand(bandId);
        requireHost(band, userId);
        BandApplication app = pendingApplication(bandId, applicationId);
        List<BandMember> list = members.findByBandWithUser(bandId);
        if (list.stream().anyMatch(m -> m.getUser().getId().equals(app.getUser().getId()))) {
            throw conflict("이미 이 밴드 멤버예요");
        }
        if (openSeats(band, list, app.getInstrument()) <= 0) {
            throw conflict(app.getInstrument().label() + " 빈자리가 방금 찼어요. 자리를 늘린 뒤 수락해 주세요");
        }
        members.save(new BandMember(band, app.getUser(), app.getInstrument(), MemberRole.MEMBER));
        app.decide(ACCEPTED);
        return detail(userId, bandId);
    }

    public BandDetail reject(Long userId, Long bandId, Long applicationId) {
        Band band = lockBand(bandId);
        requireHost(band, userId);
        pendingApplication(bandId, applicationId).decide(REJECTED);
        return detail(userId, bandId);
    }

    public void leave(Long userId, Long bandId) {
        lockBand(bandId);
        BandMember m = members.findByBandIdAndUserId(bandId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이 밴드 멤버가 아니에요"));
        if (m.getRole() == MemberRole.HOST) {
            throw conflict("방장은 아직 나갈 수 없어요. 방장 넘기기는 다음 업데이트에서 열려요");
        }
        members.delete(m);
    }

    // ───────── 내부 도우미 ─────────

    private User onboarded(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"));
        if (!u.isOnboarded()) throw new ApiException(HttpStatus.FORBIDDEN, "프로필을 먼저 설정해 주세요");
        return u;
    }

    private Band lockBand(Long bandId) {
        return bands.findByIdForUpdate(bandId).orElseThrow(BandService::notFound);
    }

    private static void requireHost(Band band, Long userId) {
        if (!band.isHost(userId)) throw new ApiException(HttpStatus.FORBIDDEN, "방장만 할 수 있어요");
    }

    private BandApplication pendingApplication(Long bandId, Long applicationId) {
        BandApplication app = applications.findById(applicationId)
                .filter(a -> a.getBand().getId().equals(bandId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "지원서를 찾을 수 없어요"));
        if (!app.isPending()) throw conflict("이미 처리된 지원이에요");
        return app;
    }

    static void validateSeats(Map<Instrument, Integer> seats, Map<Instrument, Integer> filled) {
        int total = 0;
        for (var e : seats.entrySet()) {
            int n = e.getValue();
            if (n < 0) throw bad("자리 수가 올바르지 않아요");
            if (n > Band.MAX_PER_INSTRUMENT) throw bad("한 악기는 " + Band.MAX_PER_INSTRUMENT + "자리까지 만들 수 있어요");
            total += n;
        }
        if (total > Band.MAX_SEATS) throw bad("자리는 모두 " + Band.MAX_SEATS + "개까지 만들 수 있어요");
        for (var e : filled.entrySet()) {
            if (seats.getOrDefault(e.getKey(), 0) < e.getValue()) {
                throw conflict(e.getKey().label() + " 자리는 이미 멤버가 있어 " + e.getValue() + "개보다 줄일 수 없어요");
            }
        }
    }

    private static Map<Instrument, Integer> filledCounts(List<BandMember> list) {
        Map<Instrument, Integer> m = new EnumMap<>(Instrument.class);
        list.forEach(x -> m.merge(x.getInstrument(), 1, Integer::sum));
        return m;
    }

    private static int openSeats(Band band, List<BandMember> list, Instrument instrument) {
        return band.seatCount(instrument) - filledCounts(list).getOrDefault(instrument, 0);
    }

    private static List<SeatView> seatViews(Band band, List<BandMember> list) {
        Map<Instrument, Integer> filled = filledCounts(list);
        return Arrays.stream(Instrument.values())
                .filter(i -> band.seatCount(i) > 0 || filled.containsKey(i))
                .map(i -> new SeatView(i, Math.max(band.seatCount(i), filled.getOrDefault(i, 0)), filled.getOrDefault(i, 0)))
                .toList();
    }

    private Map<Long, List<BandMember>> membersByBand(List<Band> list) {
        if (list.isEmpty()) return Map.of();
        return members.findByBandIds(list.stream().map(Band::getId).toList()).stream()
                .collect(Collectors.groupingBy(m -> m.getBand().getId()));
    }

    private static MyRelation relation(List<BandMember> list, Long userId, boolean pending) {
        return list.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst()
                .map(m -> m.getRole() == MemberRole.HOST ? MyRelation.HOST : MyRelation.MEMBER)
                .orElse(pending ? MyRelation.PENDING : MyRelation.NONE);
    }

    private static BandSummary summary(Band b, List<BandMember> list, Long userId, boolean pending, Integer pendingCount,
                                       JamBrief nextJam) {
        Instrument myInst = list.stream().filter(m -> m.getUser().getId().equals(userId))
                .map(BandMember::getInstrument).findFirst().orElse(null);
        return new BandSummary(b.getId(), b.getTitle(), b.getGenre(), b.getLevel(), b.getArea(), b.getSchedule(),
                b.isRecruiting(), b.getHost().getNickname(), list.size(), seatViews(b, list),
                relation(list, userId, pending), myInst, pendingCount, nextJam);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ApiException bad(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ApiException conflict(String msg) {
        return new ApiException(HttpStatus.CONFLICT, msg);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요");
    }
}
