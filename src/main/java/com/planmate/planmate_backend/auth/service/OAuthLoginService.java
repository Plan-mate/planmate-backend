package com.planmate.planmate_backend.auth.service;

import com.planmate.planmate_backend.auth.jwt.JwtUtil;
import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.common.util.TokenHasher;
import com.planmate.planmate_backend.auth.repository.UserRepository;
import com.planmate.planmate_backend.auth.dto.KakaoProfileDto;
import com.planmate.planmate_backend.auth.dto.JwtTokenResDto;
import com.planmate.planmate_backend.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final KakaoOAuthService kakaoOAuthService;

    @Transactional
    public JwtTokenResDto loginWithKakao(String code) {
        KakaoProfileDto kakaoProfile = kakaoOAuthService.getUserInfo(code);

        User user = userRepository.findByKakaoId(kakaoProfile.getId())
                .orElseGet(() -> registerUser(kakaoProfile));

        JwtTokenResDto tokens = issueTokens(user);

        updateRefreshToken(user, tokens.getRefreshToken());

        return tokens;
    }

    public JwtTokenResDto issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getNickname());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new JwtTokenResDto(accessToken, refreshToken);
    }

    public void updateRefreshToken(User user, String refreshToken) {
        try {
            String salt = TokenHasher.generateSalt();
            String hashedToken = TokenHasher.hashToken(refreshToken, salt);
            userRepository.updateRefreshToken(user.getId(), hashedToken, salt);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 해싱 실패");
        }
    }

    private User registerUser(KakaoProfileDto kakaoUser) {
        User newUser = User.builder()
                .kakaoId(kakaoUser.getId())
                .nickname(kakaoUser.getNickname())
                .profileImage(kakaoUser.getProfileImage())
                .build();
        return userRepository.save(newUser);
    }
}
