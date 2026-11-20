package com.bandmong.notification;

import com.bandmong.notification.dto.NotificationPage;
import com.bandmong.notification.dto.NotificationPush;
import com.bandmong.notification.dto.NotificationView;
import com.bandmong.stream.AfterCommit;
import com.bandmong.stream.SseHub;
import com.bandmong.user.User;
import com.bandmong.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    static final int DEFAULT_PAGE = 20;

    private final NotificationRepository repo;
    private final SseHub hub;
    private final Clock clock;

    public NotificationService(NotificationRepository repo, SseHub hub, Clock clock) {
        this.repo = repo;
        this.hub = hub;
        this.clock = clock;
    }

    /** 알림을 저장하고, 커밋된 뒤에 열려 있는 화면으로 밀어 준다 */
    public void notify(Collection<User> to, NotificationType type, String title, String body, String link) {
        for (User u : to) {
            Notification saved = repo.save(new Notification(u, type, title, body, link));
            NotificationView view = NotificationView.of(saved);
            Long userId = u.getId();
            AfterCommit.run(() -> hub.send(userId, "notification", new NotificationPush(view, repo.countUnread(userId))));
        }
    }

    @Transactional(readOnly = true)
    public NotificationPage page(Long userId, Long before, Integer limit) {
        int size = limit == null ? DEFAULT_PAGE : Math.max(1, Math.min(50, limit));
        List<Notification> list = repo.findPage(userId, before == null ? Long.MAX_VALUE : before, PageRequest.of(0, size + 1));
        boolean hasMore = list.size() > size;
        return new NotificationPage(list.subList(0, Math.min(size, list.size())).stream().map(NotificationView::of).toList(),
                hasMore, repo.countUnread(userId));
    }

    public NotificationView read(Long userId, Long id) {
        Notification n = repo.findById(id)
                .filter(x -> x.getUser().getId().equals(userId)) // 남의 알림은 없는 것처럼
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없어요"));
        n.markRead();
        return NotificationView.of(n);
    }

    public Map<String, Long> readAll(Long userId) {
        repo.markAllRead(userId, clock.instant());
        return Map.of("unread", 0L);
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) {
        return repo.countUnread(userId);
    }
}
