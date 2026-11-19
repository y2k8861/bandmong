package com.bandmong.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 로그인 화면이 어떤 버튼을 보여줄지 알려준다. 이 요청이 CSRF 쿠키도 받아 둔다. */
@RestController
public class AuthController {

    private final ObjectProvider<ClientRegistrationRepository> clients;
    private final boolean devLogin;

    public AuthController(ObjectProvider<ClientRegistrationRepository> clients,
                          @Value("${bandmong.dev-login:false}") boolean devLogin) {
        this.clients = clients;
        this.devLogin = devLogin;
    }

    @GetMapping("/api/auth/providers")
    public Map<String, Object> providers() {
        List<String> ids = new ArrayList<>();
        if (clients.getIfAvailable() instanceof Iterable<?> regs) {
            for (Object r : regs) {
                if (r instanceof ClientRegistration cr) ids.add(cr.getRegistrationId());
            }
        }
        return Map.of("providers", ids, "devLogin", devLogin);
    }
}
