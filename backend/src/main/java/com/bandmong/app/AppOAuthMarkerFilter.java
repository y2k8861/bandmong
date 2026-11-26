package com.bandmong.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 앱이 시스템 브라우저로 /oauth2/authorization/kakao?app=1 을 열면 "앱에서 시작한 로그인"이라고 세션에 적어 둔다.
 * 로그인이 끝나면 OAuthLoginSuccessHandler가 이 표시를 보고 웹 화면 대신 bandmong://auth?code=… 로 돌려보낸다.
 */
public class AppOAuthMarkerFilter extends OncePerRequestFilter {

    public static final String ATTR = "BANDMONG_APP_LOGIN";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/oauth2/authorization/") && "1".equals(req.getParameter("app"))) {
            req.getSession(true).setAttribute(ATTR, Boolean.TRUE);
        }
        chain.doFilter(req, res);
    }
}
