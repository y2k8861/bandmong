package com.bandmong.user;

import com.bandmong.moderation.AccountGuard;
import com.bandmong.web.ProfanityFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountGuard guard;
    private final AccountService accounts;
    private final ProfanityFilter profanity;

    public MeController(AccountGuard guard, AccountService accounts, ProfanityFilter profanity) {
        this.guard = guard;
        this.accounts = accounts;
        this.profanity = profanity;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return MeResponse.of(accounts.active(userId), guard.restrictedUntil(userId).orElse(null));
    }

    /** 탈퇴. 성공하면 세션도 끊는다. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId, HttpServletRequest request) {
        accounts.withdraw(userId);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    @PutMapping("/profile")
    public MeResponse updateProfile(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ProfileRequest req) {
        profanity.check(req.nickname());
        profanity.check(req.region());
        return MeResponse.of(accounts.updateProfile(userId, req));
    }
}
