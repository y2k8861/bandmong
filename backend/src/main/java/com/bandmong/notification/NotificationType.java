package com.bandmong.notification;

public enum NotificationType {
    APPLICATION_RECEIVED,   // 방장: 새 지원자
    APPLICATION_ACCEPTED,   // 지원자: 수락됨
    APPLICATION_REJECTED,   // 지원자: 거절됨
    MEMBER_LEFT,            // 방장: 멤버가 나감
    JAM_SCHEDULED,          // 멤버: 합주가 잡힘
    JAM_MOVED,              // 멤버: 합주 시간이 바뀜 (응답 다시)
    JAM_EDITED,             // 멤버: 장소·메모가 바뀜
    JAM_CANCELED,           // 멤버: 합주 취소
    BOOKING_CONFIRMED,      // 멤버: 합주실 예약 + 정산 금액
    BOOKING_CANCELED,       // 멤버: 합주실 예약 취소
    GIG_SCHEDULED,          // 멤버: 공연이 잡힘
    HOST_CHANGED,           // 새 방장
    REPORT_RESOLVED,        // 신고한 사람: 처리 결과
    SANCTIONED              // 신고된 사람: 경고·이용 제한·메시지 가림
}
