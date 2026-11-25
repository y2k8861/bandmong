package com.bandmong.moderation;

import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 모든 API 요청 앞에서 계정 상태를 본다.
 * - 탈퇴한 계정의 남은 세션(다른 기기) → 401
 * - 이용 정지 중 → 조회(GET)만 허용, 쓰기는 403 "○월 ○일까지 이용이 제한돼요"
 * 로그아웃·탈퇴는 정지 중에도 할 수 있다.
 */
@Component
public class AccountGuard implements HandlerInterceptor {

    static final DateTimeFormatter UNTIL = DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final UserRepository users;
    private final SanctionRepository sanctions;
    private final Clock clock;

    public AccountGuard(UserRepository users, SanctionRepository sanctions, Clock clock) {
        this.users = users;
        this.sanctions = sanctions;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) return true;

        Optional<User> user = users.findById(userId);
        if (user.isEmpty() || user.get().isDeleted()) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "탈퇴했거나 없는 계정이에요");
        }
        String method = request.getMethod();
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) return true;
        String path = request.getRequestURI();
        if (path.equals("/api/me") && method.equals("DELETE")) return true; // 정지 중에도 탈퇴는 가능

        Optional<Instant> until = restrictedUntil(userId);
        if (until.isPresent()) {
            throw new ApiException(HttpStatus.FORBIDDEN, UNTIL.format(until.get()) + "까지 이용이 제한돼요. 보기만 할 수 있어요");
        }
        return true;
    }

    public Optional<Instant> restrictedUntil(Long userId) {
        Instant now = clock.instant();
        List<Sanction> list = sanctions.findActive(userId, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0).getEndsAt());
    }
}
