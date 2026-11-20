package com.bandmong.notification.dto;

/** 실시간으로 밀어 주는 모양: 새 알림 + 안 읽은 수 */
public record NotificationPush(NotificationView item, long unread) {
}
