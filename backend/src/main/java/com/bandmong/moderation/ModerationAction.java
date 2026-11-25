package com.bandmong.moderation;

/** 관리자가 신고를 처리하는 방법 */
public enum ModerationAction {
    DISMISS,          // 문제 없음
    WARN,             // 경고 (기록 + 알림)
    SUSPEND_7,        // 7일 이용 제한
    SUSPEND_30,       // 30일 이용 제한
    HIDE_MESSAGE,     // 메시지 가리기 (메시지 신고만)
    CLOSE_RECRUITING  // 밴드 모집 닫기 (밴드 신고만)
}
