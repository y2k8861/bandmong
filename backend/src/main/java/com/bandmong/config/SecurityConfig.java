package com.bandmong.config;

import com.bandmong.auth.OAuthLoginSuccessHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 인증 방식: 세션 쿠키(JSESSIONID) + CSRF 쿠키(XSRF-TOKEN).
 * - 토큰(JWT)을 브라우저 저장소에 두지 않아 XSS로 토큰이 털릴 걱정이 줄어든다.
 * - 쓰기 요청(POST/PUT/DELETE)은 X-XSRF-TOKEN 헤더가 있어야 통과한다.
 * - 로그인이 필요한 API에 그냥 들어오면 로그인 페이지로 보내지 않고 401만 돌려준다 (SPA가 처리).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    ObjectProvider<ClientRegistrationRepository> clients,
                                    OAuthLoginSuccessHandler oauthSuccess) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // SSE 연결이 끝날 때 서버 안에서 다시 들어오는 비동기·오류 요청은 막지 않는다
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**", "/api/time", "/api/dev/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        // 소셜 로그인 키가 설정된 환경(운영)에서만 켠다. 로컬에서는 개발용 로그인을 쓴다.
        if (clients.getIfAvailable() != null) {
            http.oauth2Login(o -> o.successHandler(oauthSuccess));
        }
        return http.build();
    }
}
