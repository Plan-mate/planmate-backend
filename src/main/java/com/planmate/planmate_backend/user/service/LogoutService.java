package com.planmate.planmate_backend.user.service;

import com.planmate.planmate_backend.auth.service.RefreshTokenService;
import com.planmate.planmate_backend.user.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final FcmTokenService fcmTokenService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public ResponseEntity<Void> logout(Long userId) {
        refreshTokenService.clearRefreshToken(userId);
        fcmTokenService.clearFcmToken(userId);
        return ResponseEntity.noContent().build();
    }
}
