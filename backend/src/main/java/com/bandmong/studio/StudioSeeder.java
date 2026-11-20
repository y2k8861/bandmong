package com.bandmong.studio;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 합주실 예시 데이터 (프로토타입과 같은 7곳). 비어 있을 때만 넣는다. */
@Component
public class StudioSeeder implements ApplicationRunner {

    private final StudioRepository studios;

    public StudioSeeder(StudioRepository studios) {
        this.studios = studios;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (studios.count() > 0) return;
        studios.save(new Studio("합정 그루브 합주실", "마포·홍대", "합정역 5번 출구 도보 3분", 10, 26, true, "22시 이후 10% 할인|주차 1대")
                .room("A", "A룸", 6, 20000, "드럼 세트 · 기타앰프 2대 · 베이스앰프 · 키보드 1대")
                .room("B", "B룸", 5, 22000, "드럼 세트 · 기타앰프 2대 · 베이스앰프 · 보컬 모니터 2개"));
        studios.save(new Studio("홍대 사운드랩", "마포·홍대", "홍대입구역 9번 출구 도보 5분", 12, 26, false, "녹음 장비 대여|케이블 무료")
                .room("3", "3번방", 6, 18000, "드럼 세트 · 기타앰프 2대 · 베이스앰프 · 신스 1대")
                .room("5", "5번방", 8, 26000, "드럼 세트 · 기타앰프 3대 · 베이스앰프 · 88건반 키보드"));
        studios.save(new Studio("을지로 루프 합주실", "중구·을지로", "을지로3가역 1번 출구 도보 4분", 11, 24, false, "라커 무료|퇴근길 역 가까움")
                .room("R", "루프룸", 6, 21000, "드럼 세트 · 기타앰프 2대 · 베이스앰프 · 루퍼 페달"));
        studios.save(new Studio("망원 어쿠스틱룸", "마포·망원", "망원역 2번 출구 도보 7분", 10, 23, false, "어쿠스틱 전용|카혼 비치")
                .room("S", "소형룸", 4, 15000, "어쿠스틱 기타앰프 · 카혼 · 보컬 PA"));
        studios.save(new Studio("강남 비트박스 스튜디오", "강남·서초", "강남역 11번 출구 도보 6분", 10, 26, true, "22시 이후 10% 할인|음료 무료")
                .room("A", "A룸", 7, 24000, "드럼 세트 · 기타앰프 3대 · 베이스앰프 · 키보드 2대")
                .room("B", "B룸", 5, 21000, "드럼 세트 · 기타앰프 2대 · 베이스앰프"));
        studios.save(new Studio("판교 테크노밸리 연습실", "판교·분당", "판교역 3번 출구 도보 8분", 9, 24, false, "주차 2시간 무료")
                .room("1", "1번방", 6, 19000, "드럼 세트 · 기타앰프 2대 · 베이스앰프 · 키보드 1대"));
        studios.save(new Studio("성수 언더그라운드", "성동·성수", "성수역 3번 출구 도보 5분", 12, 26, true, "22시 이후 10% 할인|공연 영상 촬영 대여")
                .room("L", "라이브룸", 10, 32000, "드럼 세트 · 기타앰프 3대 · 베이스앰프 · 무대 조명 · PA 시스템")
                .room("M", "M룸", 5, 20000, "드럼 세트 · 기타앰프 2대 · 베이스앰프"));
    }
}
