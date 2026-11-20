package com.bandmong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 아이디·비밀번호 로그인은 쓰지 않으므로 기본 사용자 자동 생성은 끈다.
@EnableScheduling // SSE 연결 유지 신호
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BandmongApplication {

    public static void main(String[] args) {
        SpringApplication.run(BandmongApplication.class, args);
    }
}
