package com.bandmong.auth;

import com.bandmong.user.MeResponse;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 개발용 로그인: 이름만 넣으면 그 이름의 테스트 계정으로 로그인된다.
 * bandmong.dev-login=true 일 때만 존재한다 (운영에서는 아예 등록되지 않음).
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "bandmong.dev-login", havingValue = "true")
public class DevAuthController {

    public record DevLoginRequest(
            @NotBlank(message = "이름을 입력해 주세요")
            @Size(max = 20, message = "이름은 20자 이내로 적어 주세요")
            String name) {
    }

    private final UserRepository users;
    private final SessionLogin sessionLogin;

    public DevAuthController(UserRepository users, SessionLogin sessionLogin) {
        this.users = users;
        this.sessionLogin = sessionLogin;
    }

    @PostMapping("/login")
    @Transactional
    public MeResponse login(@Valid @RequestBody DevLoginRequest req,
                            HttpServletRequest request, HttpServletResponse response) {
        String key = req.name().trim();
        User user = users.findByProviderAndProviderId("dev", key)
                .orElseGet(() -> users.save(new User("dev", key, null)));
        sessionLogin.login(user.getId(), request, response);
        return MeResponse.of(user);
    }
}
