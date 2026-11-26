package com.bandmong.app;

import com.bandmong.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Apple 공개키(JWKS)로 서명을 확인한다. 키 목록은 처음 쓸 때 받아 오고 캐시한다.
 * aud가 우리 앱 Bundle ID가 아니면 다른 앱에서 받은 토큰이므로 거절한다.
 */
@Component
public class NimbusAppleTokenVerifier implements AppleTokenVerifier {

    static final String ISSUER = "https://appleid.apple.com";
    static final String JWKS = "https://appleid.apple.com/auth/keys";

    private final NimbusJwtDecoder decoder;

    public NimbusAppleTokenVerifier(@Value("${bandmong.app.apple-bundle-id}") String bundleId) {
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(bundleId))));
    }

    @Override
    public AppleIdentity verify(String identityToken) {
        try {
            Jwt jwt = decoder.decode(identityToken);
            return new AppleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"));
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Apple 로그인을 확인하지 못했어요. 다시 시도해 주세요");
        }
    }
}
