package com.bandmong.moderation;

import com.bandmong.band.Band;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.chat.ChatMessage;
import com.bandmong.chat.ChatMessageRepository;
import com.bandmong.chat.MessageType;
import com.bandmong.moderation.dto.ReportRequest;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

@Service
@Transactional
public class ReportService {

    /** 하루에 신고할 수 있는 횟수 (장난·보복 신고 남발 방지) */
    static final int DAILY_LIMIT = 10;

    private final ReportRepository reports;
    private final UserRepository users;
    private final BandRepository bands;
    private final BandMemberRepository members;
    private final ChatMessageRepository messages;
    private final Clock clock;

    public ReportService(ReportRepository reports, UserRepository users, BandRepository bands,
                         BandMemberRepository members, ChatMessageRepository messages, Clock clock) {
        this.reports = reports;
        this.users = users;
        this.bands = bands;
        this.members = members;
        this.messages = messages;
        this.clock = clock;
    }

    public Map<String, Object> report(Long userId, ReportRequest r) {
        User me = users.findById(userId).orElseThrow();
        if (reports.countSince(userId, clock.instant().minus(Duration.ofDays(1))) >= DAILY_LIMIT) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "오늘은 더 신고할 수 없어요. 급한 일이면 고객센터로 알려 주세요");
        }
        Long accused = switch (r.targetType()) {
            case USER -> {
                User u = users.findById(r.targetId()).filter(x -> !x.isDeleted()).orElseThrow(ReportService::notFound);
                yield u.getId();
            }
            case BAND -> {
                Band b = bands.findById(r.targetId()).orElseThrow(ReportService::notFound);
                yield b.getHost().getId();
            }
            case MESSAGE -> {
                ChatMessage m = messages.findById(r.targetId()).orElseThrow(ReportService::notFound);
                if (m.getType() != MessageType.TEXT || m.getSender() == null) throw bad("안내 메시지는 신고할 수 없어요");
                // 메시지는 그 밴드 멤버만 볼 수 있으니 신고도 멤버만
                if (members.findByBandIdAndUserId(m.getBand().getId(), userId).isEmpty()) throw notFound();
                yield m.getSender().getId();
            }
        };
        if (accused.equals(userId)) throw bad("나 자신이나 내 밴드·메시지는 신고할 수 없어요");
        if (reports.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(userId, r.targetType(), r.targetId(), ReportStatus.OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 신고했어요. 운영팀이 확인 중이에요");
        }
        String detail = r.detail() == null || r.detail().isBlank() ? null : r.detail().trim();
        Report saved = reports.save(new Report(me, r.targetType(), r.targetId(), accused, r.reason(), detail));
        return Map.of("id", saved.getId(), "message", "신고했어요. 운영팀이 확인하고 결과를 알려 드릴게요");
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "신고할 대상을 찾을 수 없어요");
    }

    private static ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
