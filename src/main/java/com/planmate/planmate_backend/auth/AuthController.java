package com.planmate.planmate_backend.auth;

import com.planmate.planmate_backend.auth.dto.KakaoAuthCodeDto;
import com.planmate.planmate_backend.auth.dto.JwtTokenDto;
import com.planmate.planmate_backend.auth.service.OAuthLoginService;
import com.planmate.planmate_backend.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthLoginService oAuthLoginService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/kakao")
    public JwtTokenDto kakaoLogin(@RequestBody KakaoAuthCodeDto dto) {
        return oAuthLoginService.loginWithKakao(dto.getToken());
    }

    @PostMapping("/refresh")
    public JwtTokenDto refreshToken(@RequestHeader("Authorization") String refreshToken) {
        return refreshTokenService.refreshToken(refreshToken);
    }
}