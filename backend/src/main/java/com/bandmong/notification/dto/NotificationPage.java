package com.bandmong.notification.dto;

import java.util.List;

public record NotificationPage(List<NotificationView> items, boolean hasMore, long unread) {
}
