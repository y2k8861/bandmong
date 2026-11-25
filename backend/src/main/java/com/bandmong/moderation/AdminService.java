package com.bandmong.moderation;

import com.bandmong.band.Band;
import com.bandmong.band.BandRepository;
import com.bandmong.chat.ChatMessage;
import com.bandmong.chat.ChatMessageRepository;
import com.bandmong.moderation.dto.ReportView;
import com.bandmong.moderation.dto.ResolveRequest;
import com.bandmong.notification.NotificationService;
import com.bandmong.notification.NotificationType;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.bandmong.moderation.ModerationAction.*;

@Service
@Transactional
public class AdminService {

    private final ReportRepository reports;
    private final SanctionRepository sanctions;
    private final UserRepository users;
    private final BandRepository bands;
    private final ChatMessageRepository messages;
    private final NotificationService notifications;
    private final AccountGuard guard;
    private final Clock clock;

    public AdminService(ReportRepository reports, SanctionRepository sanctions, UserRepository users, BandRepository bands,
                        ChatMessageRepository messages, NotificationService notifications, AccountGuard guard, Clock clock) {
        this.reports = reports;
        this.sanctions = sanctions;
        this.users = users;
        this.bands = bands;
        this.messages = messages;
        this.notifications = notifications;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReportView> list(ReportStatus status) {
        return reports.findByStatus(status, PageRequest.of(0, 100)).stream().map(this::view).toList();
    }

    /**
     * 신고 처리. 같은 대상에 열린 신고가 여러 개면 한 번에 같은 결과로 닫는다
     * (같은 메시지를 다섯 명이 신고했으면 처리도 알림도 다섯 명 모두에게).
     */
    public ReportView resolve(Long adminId, Long reportId, ResolveRequest r) {
        Report report = reports.findById(reportId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없어요"));
        if (report.getStatus() != ReportStatus.OPEN) throw new ApiException(HttpStatus.CONFLICT, "이미 처리된 신고예요");
        ModerationAction a = r.action();
        if (a == HIDE_MESSAGE && report.getTargetType() != ReportTarget.MESSAGE) throw bad("메시지 신고에서만 가릴 수 있어요");
        if (a == CLOSE_RECRUITING && report.getTargetType() != ReportTarget.BAND) throw bad("밴드 신고에서만 모집을 닫을 수 있어요");
        if (report.getAccusedId().equals(adminId) && a != DISMISS) throw bad("자기 자신은 제재할 수 없어요");

        Instant now = clock.instant();
        User accused = users.findById(report.getAccusedId()).orElseThrow();
        String why = report.getReason().label();
        switch (a) {
            case DISMISS -> { }
            case WARN -> {
                sanctions.save(new Sanction(accused.getId(), SanctionType.WARNING, now, now, why, reportId, adminId));
                tell(accused, "운영 정책 안내", "신고가 접수되어 경고 처리됐어요 (" + why + "). 반복되면 이용이 제한될 수 있어요");
            }
            case SUSPEND_7, SUSPEND_30 -> {
                int days = a == SUSPEND_7 ? 7 : 30;
                sanctions.save(new Sanction(accused.getId(), SanctionType.SUSPENSION, now, now.plus(Duration.ofDays(days)), why, reportId, adminId));
                tell(accused, "이용이 제한됐어요", days + "일 동안 보기만 할 수 있어요 (" + why + ")");
            }
            case HIDE_MESSAGE -> {
                ChatMessage m = messages.findById(report.getTargetId()).orElseThrow();
                m.hide();
                tell(accused, "메시지가 가려졌어요", "신고된 메시지가 운영 정책에 따라 가려졌어요 (" + why + ")");
            }
            case CLOSE_RECRUITING -> {
                Band b = bands.findById(report.getTargetId()).orElseThrow();
                b.updateInfo(null, null, false);
                tell(accused, "밴드 모집이 닫혔어요", "'" + b.getTitle() + "' 모집 글이 운영 정책에 따라 닫혔어요 (" + why + ")");
            }
        }
        ReportStatus closed = a == DISMISS ? ReportStatus.DISMISSED : ReportStatus.RESOLVED;
        String note = r.note() == null || r.note().isBlank() ? null : r.note().trim();
        for (Report same : reports.findByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), ReportStatus.OPEN)) {
            same.close(closed, a.name(), adminId, note, now);
            notifications.notify(List.of(same.getReporter()), NotificationType.REPORT_RESOLVED, "신고를 확인했어요",
                    a == DISMISS ? "운영 정책 위반은 찾지 못했어요. 알려 주셔서 고마워요"
                            : "조치를 했어요. 알려 주셔서 고마워요", null);
        }
        reports.flush();
        return view(report);
    }

    /** 이용 제한 일찍 풀기 */
    public Map<String, Object> lift(Long adminId, Long userId) {
        Instant now = clock.instant();
        List<Sanction> active = sanctions.findActive(userId, now);
        active.forEach(s -> s.lift(now));
        if (!active.isEmpty()) {
            users.findById(userId).ifPresent(u -> tell(u, "이용 제한이 풀렸어요", "다시 모든 기능을 쓸 수 있어요"));
        }
        return Map.of("lifted", active.size());
    }

    private void tell(User u, String title, String body) {
        notifications.notify(List.of(u), NotificationType.SANCTIONED, title, body, "/me");
    }

    private ReportView view(Report r) {
        User accused = users.findById(r.getAccusedId()).orElse(null);
        String summary = switch (r.getTargetType()) {
            case USER -> accused == null ? "(없는 회원)" : accused.getNickname();
            case BAND -> bands.findById(r.getTargetId()).map(Band::getTitle).orElse("(없는 밴드)");
            case MESSAGE -> messages.findById(r.getTargetId())
                    .map(m -> (m.isHidden() ? "[가림] " : "") + m.getBody()).orElse("(없는 메시지)");
        };
        return new ReportView(r.getId(), r.getTargetType(), r.getTargetId(), r.getReason(), r.getReason().label(), r.getDetail(),
                r.getStatus(), r.getCreatedAt(), r.getReporter().getNickname(), r.getAccusedId(),
                accused == null ? null : accused.getNickname(), summary,
                reports.findByTargetTypeAndTargetIdAndStatus(r.getTargetType(), r.getTargetId(), ReportStatus.OPEN).size(),
                reports.countAgainst(r.getAccusedId()), guard.restrictedUntil(r.getAccusedId()).orElse(null),
                r.getResolution(), r.getAdminNote());
    }

    private static ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
