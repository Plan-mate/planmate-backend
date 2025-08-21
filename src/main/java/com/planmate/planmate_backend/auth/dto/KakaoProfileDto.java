package com.planmate.planmate_backend.auth.dto;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KakaoProfileDto {
    private Long id;
    private String nickname;
    private String profileImage;
}