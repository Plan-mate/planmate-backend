package com.planmate.planmate_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FcmTokenDto {

    @NotBlank(message = "FCM 토큰은 비어 있을 수 없습니다.")
    private String fcmToken;
}
