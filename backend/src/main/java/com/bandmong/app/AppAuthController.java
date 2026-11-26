package com.bandmong.app;

import com.bandmong.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 앱 전용 로그인·기기 등록. 로그인 응답의 token을 앱이 안전한 저장소에 두고 Authorization 헤더로 보낸다. */
@RestController
@RequestMapping("/api/app")
public class AppAuthController {

    public record AppleLogin(@NotBlank String identityToken, @Size(max = 60) String deviceName) {
    }

    public record DevLogin(@NotBlank @Size(max = 20) String name, @Size(max = 60) String deviceName) {
    }

    public record Exchange(@NotBlank String code, @Size(max = 60) String deviceName) {
    }

    public record Device(@NotBlank @Size(max = 255) String token, @NotNull DevicePlatform platform) {
    }

    public record Logout(@Size(max = 255) String deviceToken) {
    }

    private final AppAuthService service;
    private final boolean devLogin;

    public AppAuthController(AppAuthService service, @Value("${bandmong.dev-login:false}") boolean devLogin) {
        this.service = service;
        this.devLogin = devLogin;
    }

    @PostMapping("/login/apple")
    public AppAuthService.Issued apple(@Valid @RequestBody AppleLogin req) {
        return service.appleLogin(req.identityToken(), req.deviceName());
    }

    /** 개발·심사용 로그인 (dev-login이 켜진 서버에서만) */
    @PostMapping("/login/dev")
    public AppAuthService.Issued dev(@Valid @RequestBody DevLogin req) {
        if (!devLogin) throw new ApiException(HttpStatus.NOT_FOUND, "없는 기능이에요");
        return service.devLogin(req.name(), req.deviceName());
    }

    /** 카카오·네이버·구글 로그인을 마친 브라우저가 앱으로 넘겨준 일회용 코드 → 토큰 */
    @PostMapping("/login/exchange")
    public AppAuthService.Issued exchange(@Valid @RequestBody Exchange req) {
        return service.exchange(req.code(), req.deviceName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Long userId, HttpServletRequest request,
                       @RequestBody(required = false) Logout req) {
        String raw = AppTokenFilter.bearer(request);
        if (raw != null) service.revoke(raw);
        if (userId != null && req != null && req.deviceToken() != null) service.removeDevice(userId, req.deviceToken());
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Long userId, @Valid @RequestBody Device req) {
        service.registerDevice(userId, req.token(), req.platform());
    }
}
