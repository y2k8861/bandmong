package com.bandmong.user;

import com.bandmong.band.*;
import com.bandmong.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

import static com.bandmong.band.ApplicationStatus.CANCELED;
import static com.bandmong.band.ApplicationStatus.PENDING;

/**
 * 탈퇴.
 * - 다른 멤버가 있는 밴드의 방장이면 먼저 방장을 넘겨야 한다 (방장 없는 밴드가 생기지 않게).
 * - 혼자 남은 밴드는 모집을 닫고 나온다. 일반 멤버로 있던 밴드는 "나갔어요" 안내와 함께 나온다.
 * - 개인정보를 지우고 계정을 익명화한다. 채팅·합주 기록은 "탈퇴한 사용자"로 남는다.
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final BandMemberRepository members;
    private final BandApplicationRepository applications;
    private final BandService bandService;
    private final Clock clock;
    private final com.bandmong.app.AppAuthService appAuth;

    public AccountService(UserRepository users, BandMemberRepository members, BandApplicationRepository applications,
                          BandService bandService, Clock clock, com.bandmong.app.AppAuthService appAuth) {
        this.appAuth = appAuth;
        this.users = users;
        this.members = members;
        this.applications = applications;
        this.bandService = bandService;
        this.clock = clock;
    }

    /** 로그인한 사람의 계정. 세션은 남았는데 계정이 지워졌으면 401. */
    @Transactional(readOnly = true)
    public User active(Long userId) {
        if (userId == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요");
        return users.findById(userId).filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"));
    }

    public User updateProfile(Long userId, ProfileRequest req) {
        User u = active(userId);
        u.updateProfile(req.nickname(), req.instruments(), req.level(), req.region());
        return u;
    }

    public void withdraw(Long userId) {
        User me = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"));
        List<BandMember> mine = members.findByUserWithBand(userId);

        List<String> blocking = mine.stream()
                .filter(m -> m.getRole() == MemberRole.HOST)
                .filter(m -> members.findByBandWithUser(m.getBand().getId()).size() > 1)
                .map(m -> "'" + m.getBand().getTitle() + "'").toList();
        if (!blocking.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "방장인 밴드가 있어요: " + String.join(", ", blocking) + ". 다른 멤버에게 방장을 넘긴 뒤 탈퇴해 주세요");
        }

        for (BandMember m : mine) {
            if (m.getRole() == MemberRole.HOST) {
                m.getBand().updateInfo(null, null, false); // 혼자 남은 밴드: 모집 닫기
                members.delete(m);
            } else {
                bandService.leave(userId, m.getBand().getId());
            }
        }
        applications.findByUserAndStatus(userId, PENDING).forEach(a -> a.decide(CANCELED));
        appAuth.revokeAll(userId); // 모든 기기 로그아웃 + 푸시 끊기
        me.withdraw(clock.instant());
    }
}
