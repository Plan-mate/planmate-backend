package com.planmate.planmate_backend.user.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.user.dto.FcmTokenDto;
import com.planmate.planmate_backend.user.entity.User;
import com.planmate.planmate_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final UserRepository userRepository;

    @Transactional
    public String updateFcmToken(Long userId, FcmTokenDto reqDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));

        String newToken = reqDto.getFcmToken();

        if (newToken == null || newToken.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FCM 토큰은 비어 있을 수 없습니다.");
        }

        if (newToken.equals(user.getFcmToken())) {
            return "동일한 FCM 토큰이 이미 등록되어 있습니다.";
        }

        user.setFcmToken(newToken);
        userRepository.save(user);

        return "FCM 토큰이 성공적으로 등록/수정되었습니다.";
    }

    @Transactional
    public void clearFcmToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));

        user.setFcmToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void clearFcmTokenByValue(String token) {
        userRepository.findByFcmToken(token)
                .ifPresent(user -> {
                    user.setFcmToken(null);
                    userRepository.save(user);
                });
    }
}
