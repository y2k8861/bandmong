package com.bandmong.app;

import com.bandmong.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authorization: Bearer <앱 토큰> 이 있으면 그 요청만 로그인 상태로 만든다 (세션을 만들지 않음).
 * SSE(EventSource)는 헤더를 못 붙이므로 /api/stream 에서만 ?access_token= 을 받는다.
 * 토큰이 틀리면 다른 처리 없이 바로 401.
 */
public class AppTokenFilter extends OncePerRequestFilter {

    public static final String STREAM_PARAM = "access_token";

    private final AppAuthService auth;

    public AppTokenFilter(AppAuthService auth) {
        this.auth = auth;
    }

    public static String bearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7).trim() : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String raw = bearer(req);
        if (raw == null && "/api/stream".equals(req.getRequestURI())) raw = req.getParameter(STREAM_PARAM);
        if (raw == null || raw.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }
        Optional<User> user = auth.authenticate(raw);
        if (user.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"message\":\"다시 로그인해 주세요\"}");
            return;
        }
        User u = user.get();
        List<SimpleGrantedAuthority> roles = u.isAdmin()
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u.getId(), null, roles));
        SecurityContextHolder.setContext(ctx);
        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
