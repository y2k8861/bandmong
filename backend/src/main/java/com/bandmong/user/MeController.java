package com.bandmong.user;

import com.bandmong.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;
    private final com.bandmong.moderation.AccountGuard guard;
    private final AccountService accounts;

    public MeController(UserRepository users, com.bandmong.moderation.AccountGuard guard, AccountService accounts) {
        this.users = users;
        this.guard = guard;
        this.accounts = accounts;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return MeResponse.of(load(userId), guard.restrictedUntil(userId).orElse(null));
    }

    /** 탈퇴. 성공하면 세션도 끊는다. */
    @DeleteMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId, jakarta.servlet.http.HttpServletRequest request) {
        accounts.withdraw(userId);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @PutMapping("/profile")
    @Transactional
    public MeResponse updateProfile(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ProfileRequest req) {
        User u = load(userId);
        u.updateProfile(req.nickname(), req.instruments(), req.level(), req.region());
        return MeResponse.of(u);
    }

    private User load(Long userId) {
        if (userId == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요");
        // 세션은 남았는데 계정이 지워진 경우
        return users.findById(userId).filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"));
    }
}
