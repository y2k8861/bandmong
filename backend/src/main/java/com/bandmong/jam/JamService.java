package com.bandmong.jam;

import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.config.TimeConfig;
import com.bandmong.jam.dto.*;
import com.bandmong.user.User;
import com.bandmong.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class JamService {

    /** 가능 시간 그리드의 행 (한국 시간 시작 시). 평일 저녁 + 주말 낮을 덮는다. */
    public static final List<Integer> HOURS = List.of(14, 16, 18, 19, 20, 21);
    static final int MAX_WEEKS_AHEAD = 8;
    static final int MAX_DAYS_AHEAD = 90;
    static final int PAST_LIMIT = 20;

    private final BandRepository bands;
    private final BandMemberRepository members;
    private final JamRepository jams;
    private final JamRsvpRepository rsvps;
    private final AvailabilityRepository availabilities;
    private final Clock clock;

    public JamService(BandRepository bands, BandMemberRepository members, JamRepository jams,
                      JamRsvpRepository rsvps, AvailabilityRepository availabilities, Clock clock) {
        this.bands = bands;
        this.members = members;
        this.jams = jams;
        this.rsvps = rsvps;
        this.availabilities = availabilities;
        this.clock = clock;
    }

    // ───────── 가능 시간 투표 ─────────

    @Transactional(readOnly = true)
    public AvailabilityView availability(Long userId, Long bandId, LocalDate week) {
        LocalDate w = week == null ? thisWeek() : validWeek(week);
        List<BandMember> list = requireMember(bandId, userId);
        Map<Long, BandMember> byUser = list.stream().collect(Collectors.toMap(m -> m.getUser().getId(), m -> m));

        // 나간 사람의 표는 세지 않는다
        List<Availability> rows = availabilities.findWeek(bandId, w).stream()
                .filter(a -> byUser.containsKey(a.getUser().getId())).toList();
        Map<Integer, List<Long>> bySlot = new TreeMap<>();
        rows.forEach(a -> bySlot.computeIfAbsent(a.getSlot(), k -> new ArrayList<>()).add(a.getUser().getId()));

        Map<Integer, Integer> counts = new TreeMap<>();
        bySlot.forEach((slot, ids) -> counts.put(slot, ids.size()));
        List<Integer> mine = rows.stream().filter(a -> a.getUser().getId().equals(userId))
                .map(Availability::getSlot).sorted().toList();
        Set<Long> voted = rows.stream().map(a -> a.getUser().getId()).collect(Collectors.toSet());
        List<AvailabilityView.Voter> voters = list.stream()
                .map(m -> new AvailabilityView.Voter(m.getUser().getId(), m.getUser().getNickname(),
                        voted.contains(m.getUser().getId())))
                .toList();

        Instant now = clock.instant();
        List<AvailabilityView.Best> best = bySlot.entrySet().stream()
                .filter(e -> slotStart(w, e.getKey()).isAfter(now))
                .sorted(Comparator.<Map.Entry<Integer, List<Long>>>comparingInt(e -> -e.getValue().size())
                        .thenComparingInt(e -> e.getKey()))
                .limit(3)
                .map(e -> new AvailabilityView.Best(e.getKey(), slotStart(w, e.getKey()), e.getValue().size(),
                        e.getValue().stream().map(id -> byUser.get(id).getUser().getNickname()).toList()))
                .toList();

        return new AvailabilityView(w, HOURS, mine, counts, list.size(), voters, best);
    }

    /**
     * 내 칸을 통째로 바꾼다. 다만 "전부 지우고 다시 넣기"가 아니라 빠진 칸만 지우고 새 칸만 넣는다.
     * (Hibernate는 한 트랜잭션에서 INSERT를 DELETE보다 먼저 내보내서, 같은 칸을 지웠다 다시 넣으면 유니크 키에 걸린다)
     */
    public AvailabilityView saveAvailability(Long userId, Long bandId, AvailabilityRequest r) {
        LocalDate w = validWeek(r.weekStart());
        bands.findByIdForUpdate(bandId).orElseThrow(JamService::bandNotFound);
        List<BandMember> list = requireMember(bandId, userId);
        for (int slot : r.slots()) {
            int day = slot / 24, hour = slot % 24;
            if (slot < 0 || day > 6 || !HOURS.contains(hour)) throw bad("고를 수 없는 시간이 있어요");
        }
        BandMember me = list.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst().orElseThrow();
        List<Availability> current = availabilities.findWeek(bandId, w).stream()
                .filter(a -> a.getUser().getId().equals(userId)).toList();
        Set<Integer> have = current.stream().map(Availability::getSlot).collect(Collectors.toSet());

        availabilities.deleteAll(current.stream().filter(a -> !r.slots().contains(a.getSlot())).toList());
        availabilities.saveAll(r.slots().stream().filter(s -> !have.contains(s))
                .map(s -> new Availability(me.getBand(), me.getUser(), w, s)).toList());
        availabilities.flush();
        return availability(userId, bandId, w);
    }

    // ───────── 합주 ─────────

    public JamView create(Long userId, Long bandId, JamCreateRequest r) {
        Band band = bands.findByIdForUpdate(bandId).orElseThrow(JamService::bandNotFound);
        if (!band.isHost(userId)) throw forbidden("방장만 합주를 확정할 수 있어요");
        List<BandMember> list = members.findByBandWithUser(bandId);
        Instant start = validStart(r.startsAt());
        checkOverlap(bandId, null, start, r.durationMinutes());

        User host = memberUser(list, userId);
        Jam jam = jams.save(new Jam(band, host, start, r.durationMinutes(), blankToNull(r.place()), blankToNull(r.memo())));
        rsvps.save(new JamRsvp(jam, host, RsvpAnswer.YES)); // 확정한 방장은 참석으로 시작
        return view(jam, list, userId, true);
    }

    public JamView update(Long userId, Long jamId, JamUpdateRequest r) {
        Jam jam = lockJam(jamId);
        Band band = jam.getBand();
        if (!band.isHost(userId)) throw forbidden("방장만 바꿀 수 있어요");
        requireOpen(jam);
        List<BandMember> list = members.findByBandWithUser(band.getId());

        Instant start = r.startsAt() == null ? null : validStart(r.startsAt());
        int duration = r.durationMinutes() == null ? jam.getDurationMinutes() : r.durationMinutes();
        if (start != null || r.durationMinutes() != null) {
            checkOverlap(band.getId(), jamId, start == null ? jam.getStartsAt() : start, duration);
        }
        boolean moved = jam.reschedule(start, r.durationMinutes());
        jam.updateNote(r.place(), r.memo());
        if (moved) {
            // 다른 시간에 대한 대답이었으므로 응답을 비우고 다시 받는다
            rsvps.deleteByJam(jamId);
            rsvps.save(new JamRsvp(jam, memberUser(list, userId), RsvpAnswer.YES));
        }
        return view(jam, list, userId, true);
    }

    public JamView cancel(Long userId, Long jamId) {
        Jam jam = lockJam(jamId);
        if (!jam.getBand().isHost(userId)) throw forbidden("방장만 취소할 수 있어요");
        requireOpen(jam);
        jam.cancel();
        return view(jam, members.findByBandWithUser(jam.getBand().getId()), userId, true);
    }

    public JamView rsvp(Long userId, Long jamId, RsvpAnswer answer) {
        Jam jam = lockJam(jamId);
        List<BandMember> list = requireMember(jam.getBand().getId(), userId);
        if (jam.isCanceled()) throw conflict("취소된 합주예요");
        if (!jam.getStartsAt().isAfter(clock.instant())) throw conflict("이미 시작한 합주는 답을 바꿀 수 없어요");

        rsvps.findByJamIdAndUserId(jamId, userId).ifPresentOrElse(
                x -> x.answer(answer),
                () -> rsvps.save(new JamRsvp(jam, memberUser(list, userId), answer)));
        rsvps.flush();
        return view(jam, list, userId, true);
    }

    @Transactional(readOnly = true)
    public JamView detail(Long userId, Long jamId) {
        Jam jam = jams.findById(jamId).orElseThrow(JamService::jamNotFound);
        List<BandMember> list = requireMember(jam.getBand().getId(), userId);
        return view(jam, list, userId, true);
    }

    @Transactional(readOnly = true)
    public JamListResponse list(Long userId, Long bandId) {
        List<BandMember> list = requireMember(bandId, userId);
        Instant now = clock.instant();
        List<Jam> upcoming = jams.findUpcoming(bandId, now.minus(Duration.ofMinutes(Jam.MAX_MINUTES))).stream()
                .filter(j -> !j.isOver(now)).toList();
        List<Jam> past = jams.findPast(bandId, now, PageRequest.of(0, PAST_LIMIT)).stream()
                .filter(j -> j.isOver(now)).toList();

        List<Long> ids = new ArrayList<>();
        upcoming.forEach(j -> ids.add(j.getId()));
        past.forEach(j -> ids.add(j.getId()));
        Map<Long, List<JamRsvp>> byJam = ids.isEmpty() ? Map.of()
                : rsvps.findByJamIds(ids).stream().collect(Collectors.groupingBy(x -> x.getJam().getId()));

        Function<Jam, JamView> toView = j -> build(j, list, userId, byJam.getOrDefault(j.getId(), List.of()), false);
        return new JamListResponse(upcoming.stream().map(toView).toList(), past.stream().map(toView).toList());
    }

    /** 밴드별 다음 합주 (밴드 카드·상세용). 쿼리 두 번으로 여러 밴드를 한꺼번에. */
    @Transactional(readOnly = true)
    public Map<Long, JamBrief> nextJams(Collection<Long> bandIds, Long userId) {
        if (bandIds.isEmpty()) return Map.of();
        Map<Long, Jam> first = new LinkedHashMap<>();
        for (Jam j : jams.findUpcomingForBands(bandIds, clock.instant())) {
            first.putIfAbsent(j.getBand().getId(), j);
        }
        if (first.isEmpty()) return Map.of();
        Map<Long, RsvpAnswer> mine = rsvps.findMine(first.values().stream().map(Jam::getId).toList(), userId).stream()
                .collect(Collectors.toMap(x -> x.getJam().getId(), JamRsvp::getAnswer));
        Map<Long, JamBrief> out = new HashMap<>();
        first.forEach((bandId, j) -> out.put(bandId,
                new JamBrief(j.getId(), j.getStartsAt(), j.getDurationMinutes(), j.getPlace(), mine.get(j.getId()))));
        return out;
    }

    // ───────── 내부 도우미 ─────────

    LocalDate thisWeek() {
        return ZonedDateTime.now(clock).withZoneSameInstant(TimeConfig.KST).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate validWeek(LocalDate week) {
        if (week.getDayOfWeek() != DayOfWeek.MONDAY) throw bad("주 시작일은 월요일이어야 해요");
        LocalDate now = thisWeek();
        if (week.isBefore(now)) throw bad("지난 주는 고를 수 없어요");
        if (week.isAfter(now.plusWeeks(MAX_WEEKS_AHEAD))) throw bad(MAX_WEEKS_AHEAD + "주 뒤까지만 고를 수 있어요");
        return week;
    }

    static Instant slotStart(LocalDate week, int slot) {
        return week.plusDays(slot / 24).atTime(slot % 24, 0).atZone(TimeConfig.KST).toInstant();
    }

    private Instant validStart(OffsetDateTime at) {
        Instant start = at.toInstant(), now = clock.instant();
        if (!start.isAfter(now)) throw bad("지난 시간으로는 잡을 수 없어요");
        if (start.isAfter(now.plus(Duration.ofDays(MAX_DAYS_AHEAD)))) throw bad(MAX_DAYS_AHEAD + "일 안으로 잡아 주세요");
        return start;
    }

    private void checkOverlap(Long bandId, Long exceptJamId, Instant start, int minutes) {
        Instant end = start.plusSeconds(minutes * 60L);
        boolean clash = jams.findUpcoming(bandId, start.minus(Duration.ofMinutes(Jam.MAX_MINUTES))).stream()
                .filter(j -> !j.isCanceled() && !j.getId().equals(exceptJamId))
                .anyMatch(j -> j.getStartsAt().isBefore(end) && j.endsAt().isAfter(start));
        if (clash) throw conflict("그 시간에 이미 잡힌 합주가 있어요");
    }

    private void requireOpen(Jam jam) {
        if (jam.isCanceled()) throw conflict("취소된 합주예요");
        if (jam.isOver(clock.instant())) throw conflict("이미 끝난 합주예요");
    }

    private List<BandMember> requireMember(Long bandId, Long userId) {
        if (!bands.existsById(bandId)) throw bandNotFound();
        List<BandMember> list = members.findByBandWithUser(bandId);
        if (list.stream().noneMatch(m -> m.getUser().getId().equals(userId))) {
            throw forbidden("밴드 멤버만 볼 수 있어요");
        }
        return list;
    }

    private Jam lockJam(Long jamId) {
        return jams.findByIdForUpdate(jamId).orElseThrow(JamService::jamNotFound);
    }

    private static User memberUser(List<BandMember> list, Long userId) {
        return list.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst()
                .map(BandMember::getUser).orElseThrow(() -> forbidden("밴드 멤버만 할 수 있어요"));
    }

    private JamView view(Jam jam, List<BandMember> list, Long userId, boolean withAnswers) {
        return build(jam, list, userId, rsvps.findByJamIds(List.of(jam.getId())), withAnswers);
    }

    private JamView build(Jam jam, List<BandMember> list, Long userId, List<JamRsvp> answers, boolean withAnswers) {
        Map<Long, RsvpAnswer> byUser = answers.stream()
                .collect(Collectors.toMap(x -> x.getUser().getId(), JamRsvp::getAnswer, (a, b) -> b));
        int yes = 0, late = 0, no = 0, none = 0;
        List<RsvpView> views = new ArrayList<>();
        for (BandMember m : list) { // 지금 멤버 기준으로 센다
            RsvpAnswer a = byUser.get(m.getUser().getId());
            if (a == null) none++;
            else switch (a) {
                case YES -> yes++;
                case LATE -> late++;
                case NO -> no++;
            }
            views.add(new RsvpView(m.getUser().getId(), m.getUser().getNickname(), m.getInstrument(), a));
        }
        Band band = jam.getBand();
        return new JamView(jam.getId(), band.getId(), band.getTitle(), jam.getStartsAt(), jam.endsAt(),
                jam.getDurationMinutes(), jam.getPlace(), jam.getMemo(), jam.getStatus(), jam.isOver(clock.instant()),
                band.isHost(userId), new RsvpCounts(yes, late, no, none), byUser.get(userId),
                withAnswers ? views : null);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ApiException bad(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    private static ApiException conflict(String m) { return new ApiException(HttpStatus.CONFLICT, m); }
    private static ApiException forbidden(String m) { return new ApiException(HttpStatus.FORBIDDEN, m); }
    private static ApiException bandNotFound() { return new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요"); }
    private static ApiException jamNotFound() { return new ApiException(HttpStatus.NOT_FOUND, "합주를 찾을 수 없어요"); }
}
