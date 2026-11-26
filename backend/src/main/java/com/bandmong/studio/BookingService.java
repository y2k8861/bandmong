package com.bandmong.studio;

import com.bandmong.activity.Events;
import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.config.TimeConfig;
import com.bandmong.jam.JamRepository;
import com.bandmong.studio.dto.*;
import com.bandmong.studio.dto.BookingView.SettleView;
import com.bandmong.web.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    static final int MAX_DAYS_AHEAD = 60;

    private final StudioRepository studios;
    private final RoomRepository rooms;
    private final RoomSlotRepository slots;
    private final BookingRepository bookings;
    private final SettleRepository settles;
    private final BandRepository bands;
    private final BandMemberRepository members;
    private final JamRepository jams;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(StudioRepository studios, RoomRepository rooms, RoomSlotRepository slots,
                          BookingRepository bookings, SettleRepository settles, BandRepository bands,
                          BandMemberRepository members, JamRepository jams, ApplicationEventPublisher events, Clock clock) {
        this.studios = studios;
        this.rooms = rooms;
        this.slots = slots;
        this.bookings = bookings;
        this.settles = settles;
        this.bands = bands;
        this.members = members;
        this.jams = jams;
        this.events = events;
        this.clock = clock;
    }

    // ───────── 합주실 ─────────

    @Transactional(readOnly = true)
    public List<StudioView> studios(String area) {
        return studios.findAllWithRooms().stream()
                .filter(s -> area == null || area.isBlank() || s.getArea().equals(area))
                .map(StudioView::of).toList();
    }

    @Transactional(readOnly = true)
    public SlotsView slots(Long roomId, LocalDate date) {
        Room room = room(roomId);
        Studio st = room.getStudio();
        Instant now = clock.instant();
        Set<Instant> taken = slots.findTaken(roomId, at(date, st.getOpenHour()), at(date, st.getCloseHour())).stream()
                .map(RoomSlot::getSlotStart).collect(Collectors.toSet());
        List<SlotsView.Slot> list = new ArrayList<>();
        for (int h = st.getOpenHour(); h < st.getCloseHour(); h++) {
            Instant t = at(date, h);
            list.add(new SlotsView.Slot(h, t, st.priceAt(room, h), taken.contains(t), !t.isAfter(now)));
        }
        return new SlotsView(roomId, st.getName(), room.getName(), date, list);
    }

    // ───────── 예약 ─────────

    /**
     * 같은 방·같은 시간 겹침은 room_slots의 유니크 키가 최종적으로 막는다.
     * 밴드 행 잠금은 같은 밴드 안의 동시 요청만 줄 세울 뿐, 서로 다른 두 밴드가 같은 방을 잡는 경쟁은 못 막는다.
     */
    public BookingView book(Long userId, Long bandId, BookingRequest r) {
        Band band = bands.findByIdForUpdate(bandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요"));
        if (!band.isHost(userId)) throw forbidden("방장만 예약할 수 있어요");
        Room room = room(r.roomId());
        Studio st = room.getStudio();

        int end = r.startHour() + r.hours();
        if (r.startHour() < st.getOpenHour() || end > st.getCloseHour()) {
            throw bad("영업시간 안으로 골라 주세요 (" + hh(st.getOpenHour()) + "~" + hh(st.getCloseHour()) + ")");
        }
        Instant start = at(r.date(), r.startHour()), now = clock.instant();
        if (!start.isAfter(now)) throw bad("지난 시간은 예약할 수 없어요");
        if (start.isAfter(now.plus(Duration.ofDays(MAX_DAYS_AHEAD)))) throw bad(MAX_DAYS_AHEAD + "일 안으로 예약해 주세요");
        if (r.jamId() != null && jams.findById(r.jamId()).filter(j -> j.getBand().getId().equals(bandId)).isEmpty()) {
            throw bad("이 밴드의 합주가 아니에요");
        }
        if (!slots.findTaken(room.getId(), start, start.plusSeconds(r.hours() * 3600L)).isEmpty()) {
            throw conflict("그 시간은 이미 예약돼 있어요");
        }

        int price = 0;
        for (int h = r.startHour(); h < end; h++) price += st.priceAt(room, h);
        List<BandMember> list = members.findByBandWithUser(bandId);
        int perPerson = (int) Math.ceil(price / (double) list.size() / 100) * 100;

        Booking booking = bookings.save(new Booking(room, band, r.jamId(), start, r.hours(), price, perPerson,
                list.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst().orElseThrow().getUser()));
        try {
            List<RoomSlot> hold = new ArrayList<>();
            for (int i = 0; i < r.hours(); i++) hold.add(new RoomSlot(room.getId(), start.plusSeconds(i * 3600L), booking.getId()));
            slots.saveAllAndFlush(hold);
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            // 위의 확인과 이 저장 사이에 다른 밴드가 먼저 잡았다
            throw conflict("그 시간은 방금 다른 밴드가 예약했어요");
        }
        settles.saveAll(list.stream().map(m -> new Settle(booking, m.getUser(), perPerson)).toList());
        events.publishEvent(new Events.BookingChanged(booking.getId(), userId, true));
        return view(booking, userId);
    }

    public BookingView cancel(Long userId, Long bookingId) {
        Booking b = bookings.findByIdForUpdate(bookingId).orElseThrow(BookingService::notFound);
        if (!b.getBand().isHost(userId)) throw forbidden("방장만 취소할 수 있어요");
        if (b.isCanceled()) throw conflict("이미 취소된 예약이에요");
        if (!b.getStartsAt().isAfter(clock.instant())) throw conflict("이미 시작한 예약은 취소할 수 없어요");
        slots.release(bookingId);
        b.cancel();
        events.publishEvent(new Events.BookingChanged(bookingId, userId, false));
        return view(b, userId);
    }

    @Transactional(readOnly = true)
    public List<BookingView> list(Long userId, Long bandId) {
        requireMember(bandId, userId);
        // 정산이 남았을 수 있으니 지난 7일 것까지 보여 준다
        List<Booking> list = bookings.findForBand(bandId, clock.instant().minus(Duration.ofDays(7)));
        Map<Long, List<Settle>> byBooking = list.isEmpty() ? Map.of()
                : settles.findForBookings(list.stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getBooking().getId()));
        return list.stream().map(b -> build(b, userId, byBooking.getOrDefault(b.getId(), List.of()))).toList();
    }

    /** 나는 내 몫만, 방장은 누구 몫이든 냈다/안 냈다로 바꿀 수 있다 */
    public BookingView settle(Long userId, Long bookingId, Long targetId, boolean paid) {
        Booking b = bookings.findById(bookingId).orElseThrow(BookingService::notFound);
        requireMember(b.getBand().getId(), userId);
        if (!userId.equals(targetId) && !b.getBand().isHost(userId)) throw forbidden("다른 사람 몫은 방장만 바꿀 수 있어요");
        settles.findByBookingIdAndUserId(bookingId, targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "정산할 몫이 없어요"))
                .setPaid(paid, clock.instant());
        settles.flush();
        return view(b, userId);
    }

    // ───────── 도우미 ─────────

    private BookingView view(Booking b, Long userId) {
        return build(b, userId, settles.findForBookings(List.of(b.getId())));
    }

    private BookingView build(Booking b, Long userId, List<Settle> list) {
        List<SettleView> sv = list.stream()
                .map(s -> new SettleView(s.getUser().getId(), s.getUser().getNickname(), s.getAmount(), s.getPaidAt() != null))
                .toList();
        Room room = b.getRoom();
        Studio st = room.getStudio();
        return new BookingView(b.getId(), b.getBand().getId(), st.getName(), room.getName(), st.getArea(), st.getStation(),
                b.getStartsAt(), b.endsAt(), b.getHours(), b.getPrice(), b.getPerPerson(), b.getStatus(), b.getJamId(),
                b.getBand().isHost(userId), sv, (int) sv.stream().filter(SettleView::paid).count(),
                sv.stream().filter(s -> s.userId().equals(userId)).findFirst().orElse(null));
    }

    /** 그 날짜(한국)의 h시. 24 이상이면 다음 날 새벽으로 넘어간다. */
    static Instant at(LocalDate date, int hour) {
        return date.atStartOfDay(TimeConfig.KST).plusHours(hour).toInstant();
    }

    private static String hh(int h) {
        return String.format("%02d:00", h % 24);
    }

    private Room room(Long id) {
        return rooms.findWithStudio(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "방을 찾을 수 없어요"));
    }

    private void requireMember(Long bandId, Long userId) {
        if (members.findByBandIdAndUserId(bandId, userId).isEmpty()) throw forbidden("밴드 멤버만 볼 수 있어요");
    }

    private static ApiException bad(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    private static ApiException conflict(String m) { return new ApiException(HttpStatus.CONFLICT, m); }
    private static ApiException forbidden(String m) { return new ApiException(HttpStatus.FORBIDDEN, m); }
    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없어요"); }
}
