package com.bandmong.app;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 앱 푸시 알림 (FCM — 아이폰은 FCM이 APNs로 넘겨 준다).
 * 알림 탭에 들어가는 알림과 같은 내용을, 앱이 꺼져 있어도 보이게 보낸다.
 * bandmong.fcm-credentials(서비스 계정 JSON 경로)가 비어 있으면 아무것도 보내지 않는다 (로컬·테스트).
 * 보내기는 요청 스레드를 붙잡지 않게 가상 스레드에서 한다.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final DeviceTokenRepository devices;
    private final FirebaseMessaging messaging;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PushService(DeviceTokenRepository devices, @Value("${bandmong.fcm-credentials:}") String credentialsPath) {
        this.devices = devices;
        this.messaging = init(credentialsPath);
    }

    public boolean enabled() {
        return messaging != null;
    }

    public void send(Long userId, String title, String body, String link) {
        if (messaging == null) return;
        executor.submit(() -> {
            List<DeviceToken> list = devices.findByUserId(userId);
            for (DeviceToken d : list) {
                Message msg = Message.builder()
                        .setToken(d.getToken())
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putData("link", link == null ? "/" : link)
                        .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setSound("default").build()).build())
                        .build();
                try {
                    messaging.send(msg);
                } catch (FirebaseMessagingException e) {
                    MessagingErrorCode code = e.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        devices.deleteToken(d.getToken()); // 앱을 지웠거나 만료된 기기
                    } else {
                        log.warn("푸시 실패 user={} ({})", userId, code);
                    }
                } catch (RuntimeException e) {
                    log.warn("푸시 실패 user={}", userId, e);
                }
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private static FirebaseMessaging init(String path) {
        if (path == null || path.isBlank()) {
            log.info("FCM 설정이 없어 푸시 알림을 보내지 않습니다");
            return null;
        }
        try (var in = new FileInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            throw new IllegalStateException("FCM 서비스 계정 파일을 읽지 못했어요: " + path, e);
        }
    }
}
