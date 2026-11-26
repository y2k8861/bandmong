package com.bandmong.app;

import com.bandmong.auth.SessionLogin;
import com.bandmong.user.MeResponse;
import com.bandmong.user.User;
import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 앱 로그인.
 * 웹은 같은 주소에서 쿠키 세션을 쓰지만, 앱의 화면은 앱 안(capacitor://localhost)에서 돌아서 서버가 "다른 사이트"가 된다.
 * iOS는 이런 쿠키를 막으므로 앱은 토큰을 Authorization 헤더로 보낸다. 쿠키가 아니니 CSRF 걱정도 없다.
 */
@Service
@Transactional
public class AppAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppTokenRepository tokens;
    private final LoginCodeRepository codes;
    private final DeviceTokenRepository devices;
    private final UserRepository users;
    private final SessionLogin sessionLogin;
    private final AppleTokenVerifier apple;
    private final Clock clock;
    private final int tokenDays;

    public AppAuthService(AppTokenRepository tokens, LoginCodeRepository codes, DeviceTokenRepository devices,
                          UserRepository users, SessionLogin sessionLogin, AppleTokenVerifier apple, Clock clock,
                          @Value("${bandmong.app.token-days:180}") int tokenDays) {
        this.tokens = tokens;
        this.codes = codes;
        this.devices = devices;
        this.users = users;
        this.sessionLogin = sessionLogin;
        this.apple = apple;
        this.clock = clock;
        this.tokenDays = tokenDays;
    }

    /** 앱에 한 번만 보여 주는 토큰 원문 + 내 정보 */
    public record Issued(String token, MeResponse me) {
    }

    public Issued issue(User user, String deviceName) {
        sessionLogin.promoteIfAdmin(user);
        String raw = random(32);
        Instant now = clock.instant();
        tokens.save(new AppToken(user, sha256(raw), cut(deviceName, 60), now, now.plus(Duration.ofDays(tokenDays))));
        return new Issued(raw, MeResponse.of(user));
    }

    public Issued appleLogin(String identityToken, String deviceName) {
        AppleTokenVerifier.AppleIdentity id = apple.verify(identityToken);
        User user = users.findByProviderAndProviderId("apple", id.sub()).orElseGet(() -> {
            User u = new User("apple", id.sub(), null);
            u.setEmail(id.email()); // Apple은 "이메일 가리기"를 고르면 중계 주소를 준다
            return users.save(u);
        });
        return issue(user, deviceName);
    }

    public Issued devLogin(String name, String deviceName) {
        String key = name.trim();
        User user = users.findByProviderAndProviderId("dev", key)
                .orElseGet(() -> users.save(new User("dev", key, null)));
        return issue(user, deviceName);
    }

    /** 요청마다 불린다. 마지막 사용 시각은 한 시간에 한 번만 적어 DB 쓰기를 줄인다. */
    public Optional<User> authenticate(String raw) {
        Instant now = clock.instant();
        return tokens.findByHash(sha256(raw))
                .filter(t -> t.usableAt(now) && !t.getUser().isDeleted())
                .map(t -> {
                    if (t.getLastUsedAt().isBefore(now.minus(Duration.ofHours(1)))) t.touch(now);
                    return t.getUser();
                });
    }

    public void revoke(String raw) {
        tokens.findByHash(sha256(raw)).ifPresent(t -> t.revoke(clock.instant()));
    }

    /** 탈퇴: 모든 기기에서 로그아웃 + 푸시 끊기 */
    public void revokeAll(Long userId) {
        tokens.revokeAll(userId, clock.instant());
        devices.deleteAllOf(userId);
    }

    // ── 소셜 로그인 → 앱으로 돌아가기 ──

    public String createLoginCode(Long userId) {
        String raw = random(24);
        codes.save(new LoginCode(sha256(raw), userId, clock.instant().plus(Duration.ofMinutes(2))));
        return raw;
    }

    public Issued exchange(String code, String deviceName) {
        Instant now = clock.instant();
        LoginCode lc = codes.findByCodeHash(sha256(code)).filter(c -> c.usableAt(now))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "로그인 시간이 지났어요. 다시 로그인해 주세요"));
        lc.use(now);
        User user = users.findById(lc.getUserId()).filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "로그인할 수 없는 계정이에요"));
        return issue(user, deviceName);
    }

    // ── 푸시 기기 ──

    public void registerDevice(Long userId, String token, DevicePlatform platform) {
        Instant now = clock.instant();
        devices.findByToken(token).ifPresentOrElse(
                d -> d.reassign(userId, platform, now),
                () -> devices.save(new DeviceToken(userId, token, platform, now)));
    }

    public void removeDevice(Long userId, String token) {
        devices.findByToken(token).filter(d -> d.getUserId().equals(userId)).ifPresent(devices::delete);
    }

    static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String random(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String cut(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
