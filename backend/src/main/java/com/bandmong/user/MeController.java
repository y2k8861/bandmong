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

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return MeResponse.of(load(userId));
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
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"));
    }
}
