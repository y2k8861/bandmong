package com.bandmong.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로그인 방법(개발용·카카오·네이버·구글)과 상관없이 "세션에 우리 회원 id를 principal로 저장"하는 한 곳.
 * 컨트롤러에서는 @AuthenticationPrincipal Long userId 로 꺼내 쓴다.
 */
@Component
public class SessionLogin {

    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public void login(Long userId, HttpServletRequest request, HttpServletResponse response) {
        // 세션 고정 공격 방지: 로그인 전에 쓰던 세션 id를 바꾼다.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}
