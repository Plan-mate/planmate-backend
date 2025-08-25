package com.planmate.planmate_backend.auth.service;

import com.planmate.planmate_backend.auth.jwt.JwtUtil;
import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.common.util.TokenHasher;
import com.planmate.planmate_backend.user.repository.UserRepository;
import com.planmate.planmate_backend.user.dto.ProfileDto;
import com.planmate.planmate_backend.auth.dto.JwtTokenDto;
import com.planmate.planmate_backend.user.entity.User;
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
    public JwtTokenDto loginWithKakao(String code) {
        ProfileDto profile = kakaoOAuthService.getUserInfo(code);

        User user = userRepository.findByKakaoId(profile.getId()).orElseGet(() -> registerUser(profile));

        JwtTokenDto tokens = issueTokens(user);

        updateRefreshToken(user, tokens.getRefreshToken());

        return tokens;
    }

    public JwtTokenDto issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new JwtTokenDto(accessToken, refreshToken);
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

    private User registerUser(ProfileDto user) {
        User newUser = User.builder()
                .kakaoId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();
        return userRepository.save(newUser);
    }
}
