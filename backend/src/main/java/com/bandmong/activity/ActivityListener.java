package com.bandmong.activity;

import com.bandmong.activity.Events.*;
import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.chat.ChatService;
import com.bandmong.config.TimeConfig;
import com.bandmong.jam.Jam;
import com.bandmong.jam.JamRepository;
import com.bandmong.notification.NotificationService;
import com.bandmong.notification.NotificationType;
import com.bandmong.gig.Gig;
import com.bandmong.gig.GigRepository;
import com.bandmong.studio.Booking;
import com.bandmong.studio.BookingRepository;
import java.text.NumberFormat;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static com.bandmong.web.Korean.ro;

/**
 * 일어난 일 → 누구에게 무슨 알림, 채팅에 무슨 안내.
 * 같은 트랜잭션에서 돈다: 수락이 롤백되면 "들어왔어요" 메시지·알림도 함께 사라진다.
 * 화면으로 밀어 주는 건 각 서비스가 커밋 뒤(AfterCommit)에 한다.
 */
@Component
public class ActivityListener {

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M월 d일 (E) HH:mm", Locale.KOREAN)
            .withZone(TimeConfig.KST);
    static final DateTimeFormatter END = DateTimeFormatter.ofPattern("HH:mm").withZone(TimeConfig.KST);

    private final BandRepository bands;
    private final BandMemberRepository members;
    private final UserRepository users;
    private final JamRepository jams;
    private final ChatService chat;
    private final NotificationService notifications;
    private final BookingRepository bookings;
    private final GigRepository gigs;

    public ActivityListener(BandRepository bands, BandMemberRepository members, UserRepository users,
                            JamRepository jams, ChatService chat, NotificationService notifications,
                            BookingRepository bookings, GigRepository gigs) {
        this.bookings = bookings;
        this.gigs = gigs;
        this.bands = bands;
        this.members = members;
        this.users = users;
        this.jams = jams;
        this.chat = chat;
        this.notifications = notifications;
    }

    @EventListener
    public void on(ApplicationSubmitted e) {
        Band band = bands.findById(e.bandId()).orElseThrow();
        User who = users.findById(e.applicantId()).orElseThrow();
        notifications.notify(List.of(band.getHost()), NotificationType.APPLICATION_RECEIVED,
                "새 지원자가 있어요",
                who.getNickname() + "님이 '" + band.getTitle() + "'에 " + ro(e.instrument().label()) + " 지원했어요",
                "/bands/" + band.getId());
    }

    @EventListener
    public void on(ApplicationDecided e) {
        Band band = bands.findById(e.bandId()).orElseThrow();
        User who = users.findById(e.applicantId()).orElseThrow();
        if (e.accepted()) {
            members.findByBandIdAndUserId(e.bandId(), e.applicantId()).ifPresent(chat::startFromNow);
            chat.system(band, who.getNickname() + "님이 " + ro(e.instrument().label()) + " 들어왔어요");
            notifications.notify(List.of(who), NotificationType.APPLICATION_ACCEPTED,
                    "'" + band.getTitle() + "' 멤버가 됐어요", "밴드 채팅에서 인사해 보세요",
                    "/bands/" + band.getId() + "/chat");
        } else {
            notifications.notify(List.of(who), NotificationType.APPLICATION_REJECTED,
                    "지원 결과를 알려 드려요", "'" + band.getTitle() + "'에는 이번에 함께하기 어렵대요. 다른 밴드도 찾아보세요",
                    "/explore");
        }
    }

    @EventListener
    public void on(MemberLeft e) {
        Band band = bands.findById(e.bandId()).orElseThrow();
        User who = users.findById(e.userId()).orElseThrow();
        chat.system(band, who.getNickname() + "님이 밴드를 나갔어요");
        notifications.notify(List.of(band.getHost()), NotificationType.MEMBER_LEFT,
                who.getNickname() + "님이 나갔어요", "'" + band.getTitle() + "' " + e.instrument().label() + " 자리가 비었어요",
                "/bands/" + band.getId());
    }

    @EventListener
    public void on(HostChanged e) {
        Band band = bands.findById(e.bandId()).orElseThrow();
        User next = users.findById(e.newHostId()).orElseThrow();
        chat.system(band, next.getNickname() + "님이 새 방장이 됐어요");
        notifications.notify(List.of(next), NotificationType.HOST_CHANGED, "'" + band.getTitle() + "' 방장이 됐어요",
                "지원자 수락, 합주 확정, 합주실 예약을 이제 맡아 주세요", "/bands/" + band.getId());
    }

    @EventListener
    public void on(GigScheduled e) {
        Gig g = gigs.findById(e.gigId()).orElseThrow();
        Band band = g.getBand();
        String when = WHEN.format(g.getStartsAt()) + (g.getVenue() == null ? "" : " · " + g.getVenue());
        List<User> others = members.findByBandWithUser(band.getId()).stream()
                .map(BandMember::getUser).filter(u -> !u.getId().equals(e.actorId())).toList();
        chat.system(band, "공연이 잡혔어요 · " + g.getTitle() + " · " + when);
        notifications.notify(others, NotificationType.GIG_SCHEDULED, "공연이 잡혔어요",
                "'" + band.getTitle() + "' " + g.getTitle() + " · " + when + " · 큐시트를 같이 채워 주세요",
                "/gigs/" + g.getId());
    }

    @EventListener
    public void on(BookingChanged e) {
        Booking b = bookings.findById(e.bookingId()).orElseThrow();
        Band band = b.getBand();
        String place = b.getRoom().getStudio().getName() + " " + b.getRoom().getName();
        String when = WHEN.format(b.getStartsAt()) + "–" + END.format(b.endsAt());
        List<User> others = members.findByBandWithUser(band.getId()).stream()
                .map(BandMember::getUser).filter(u -> !u.getId().equals(e.actorId())).toList();
        if (e.confirmed()) {
            String share = "1인 " + NumberFormat.getIntegerInstance(Locale.KOREA).format(b.getPerPerson()) + "원";
            chat.system(band, "합주실을 예약했어요 · " + place + " · " + when + " · " + share);
            notifications.notify(others, NotificationType.BOOKING_CONFIRMED, "합주실이 예약됐어요",
                    place + " · " + when + " · " + share + " 정산해 주세요", "/bands/" + band.getId());
        } else {
            chat.system(band, "합주실 예약을 취소했어요 · " + place + " · " + when);
            notifications.notify(others, NotificationType.BOOKING_CANCELED, "합주실 예약이 취소됐어요",
                    place + " · " + when, "/bands/" + band.getId());
        }
    }

    @EventListener
    public void on(JamChanged e) {
        Jam jam = jams.findById(e.jamId()).orElseThrow();
        Band band = jam.getBand();
        String when = WHEN.format(jam.getStartsAt()) + (jam.getPlace() == null ? "" : " · " + jam.getPlace());
        String link = "/jams/" + jam.getId();
        List<User> others = members.findByBandWithUser(band.getId()).stream()
                .map(BandMember::getUser).filter(u -> !u.getId().equals(e.actorId())).toList();

        switch (e.change()) {
            case SCHEDULED -> {
                chat.system(band, "합주가 잡혔어요 · " + when);
                notifications.notify(others, NotificationType.JAM_SCHEDULED, "합주가 잡혔어요",
                        "'" + band.getTitle() + "' " + when + " · 참석 여부를 알려 주세요", link);
            }
            case MOVED -> {
                chat.system(band, "합주 시간이 바뀌었어요 · " + when);
                notifications.notify(others, NotificationType.JAM_MOVED, "합주 시간이 바뀌었어요",
                        "'" + band.getTitle() + "' " + when + " · 참석 여부를 다시 알려 주세요", link);
            }
            case EDITED -> notifications.notify(others, NotificationType.JAM_EDITED, "합주 정보가 바뀌었어요",
                    "'" + band.getTitle() + "' " + when, link);
            case CANCELED -> {
                chat.system(band, "합주가 취소됐어요 · " + WHEN.format(jam.getStartsAt()));
                notifications.notify(others, NotificationType.JAM_CANCELED, "합주가 취소됐어요",
                        "'" + band.getTitle() + "' " + WHEN.format(jam.getStartsAt()), link);
            }
        }
    }
}
