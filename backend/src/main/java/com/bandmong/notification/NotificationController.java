package com.bandmong.notification;

import com.bandmong.notification.dto.NotificationPage;
import com.bandmong.notification.dto.NotificationView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationPage page(@AuthenticationPrincipal Long userId,
                                 @RequestParam(required = false) Long before,
                                 @RequestParam(required = false) Integer limit) {
        return service.page(userId, before, limit);
    }

    @GetMapping("/unread")
    public Map<String, Long> unread(@AuthenticationPrincipal Long userId) {
        return Map.of("unread", service.unread(userId));
    }

    @PostMapping("/{id}/read")
    public NotificationView read(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return service.read(userId, id);
    }

    @PostMapping("/read-all")
    public Map<String, Long> readAll(@AuthenticationPrincipal Long userId) {
        return service.readAll(userId);
    }
}
