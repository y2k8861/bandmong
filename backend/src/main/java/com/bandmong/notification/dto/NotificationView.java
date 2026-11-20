package com.bandmong.notification.dto;

import com.bandmong.notification.Notification;
import com.bandmong.notification.NotificationType;

import java.time.Instant;

public record NotificationView(Long id, NotificationType type, String title, String body, String link,
                               boolean read, Instant createdAt) {
    public static NotificationView of(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }
}
