package com.planmate.planmate_backend.notification.service;

import com.google.firebase.messaging.*;
import com.planmate.planmate_backend.user.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenService fcmTokenService;

    public void sendToToken(String targetToken, String title, String body, Map<String, String> data) {
        if (targetToken == null || targetToken.isBlank()) { return; }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM 전송 실패] code={}, message={}, token={}",
                    e.getMessagingErrorCode(), e.getMessage(), targetToken, e);

            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("[FCM 토큰 정리] 만료된 토큰 감지: {}", targetToken);
                fcmTokenService.clearFcmTokenByValue(targetToken);
            }

        } catch (Exception e) {
            log.error("[FCM 예외 발생] message={}, token={}", e.getMessage(), targetToken, e);
        }
    }
}
