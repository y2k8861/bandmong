package com.bandmong.auth;

import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

/**
 * 카카오·네이버·구글 로그인 성공 후: 우리 회원을 찾거나 만들고,
 * 세션 principal을 "우리 회원 id"로 바꾼 다음 프론트로 돌려보낸다.
 * 소셜 닉네임은 가져오지 않는다 — 닉네임은 온보딩에서 직접 정한다.
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final SessionLogin sessionLogin;
    private final String frontUrl;

    public OAuthLoginSuccessHandler(UserRepository users, SessionLogin sessionLogin,
                                    @Value("${bandmong.front-url:/}") String frontUrl) {
        this.users = users;
        this.sessionLogin = sessionLogin;
        this.frontUrl = frontUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        String providerId = providerId(provider, token.getPrincipal().getAttributes());

        User user = users.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> users.save(new User(provider, providerId, null)));
        sessionLogin.login(user.getId(), request, response);
        response.sendRedirect(frontUrl);
    }

    @SuppressWarnings("unchecked")
    static String providerId(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "kakao" -> String.valueOf(attrs.get("id"));
            case "naver" -> String.valueOf(((Map<String, Object>) attrs.get("response")).get("id"));
            case "google" -> String.valueOf(attrs.get("sub"));
            default -> throw new IllegalArgumentException("지원하지 않는 로그인: " + provider);
        };
    }
}
