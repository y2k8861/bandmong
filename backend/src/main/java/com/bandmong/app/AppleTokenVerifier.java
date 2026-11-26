package com.bandmong.app;

/** 앱의 "Apple로 로그인"이 준 identityToken(JWT)을 검증하고 Apple 회원 번호(sub)를 꺼낸다. */
public interface AppleTokenVerifier {

    record AppleIdentity(String sub, String email) {
    }

    /** 서명·발급자·대상(우리 Bundle ID)·만료가 맞지 않으면 예외 */
    AppleIdentity verify(String identityToken);
}
