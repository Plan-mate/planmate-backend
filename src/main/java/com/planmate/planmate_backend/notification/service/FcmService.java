package com.planmate.planmate_backend.notification.service;

import com.google.firebase.messaging.*;
import com.planmate.planmate_backend.user.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenService fcmTokenService;

    public void sendToToken(String targetToken, String title, String body, Map<String, String> data) {
        if (targetToken == null || targetToken.isBlank()) return;

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder builder = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            FirebaseMessaging.getInstance().send(builder.build());

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                fcmTokenService.clearFcmTokenByValue(targetToken);
            }
        } catch (Exception ignored) {}
    }
}
