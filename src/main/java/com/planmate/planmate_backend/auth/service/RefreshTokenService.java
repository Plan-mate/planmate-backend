package com.planmate.planmate_backend.auth.service;

import com.planmate.planmate_backend.auth.jwt.JwtUtil;
import com.planmate.planmate_backend.common.util.TokenHasher;
import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.user.repository.UserRepository;
import com.planmate.planmate_backend.auth.dto.JwtTokenDto;
import com.planmate.planmate_backend.user.entity.User;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OAuthLoginService oAuthLoginService;

    @Transactional
    public JwtTokenDto refreshToken(String token) {
        String refreshToken = extractToken(token);

        Claims payload = jwtUtil.validateRefreshToken(refreshToken);

        User user = userRepository.findById(Long.valueOf(payload.getSubject()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "카카오 계정을 찾을 수 없습니다."));

        checkRefreshToken(refreshToken, user);

        JwtTokenDto newTokens = oAuthLoginService.issueTokens(user);

        oAuthLoginService.updateRefreshToken(user, newTokens.getRefreshToken());

        return newTokens;
    }

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "refreshToken 형식이 맞지 않습니다.");
        }
        return header.substring(7).trim();
    }

    private void checkRefreshToken(String refreshToken, User user) {
        try {
            boolean isValid = TokenHasher.verifyToken(refreshToken, user.getSalt(), user.getRefreshToken());
            if (!isValid) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레쉬 토큰이 일치하지 않습니다.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 해싱 실패");
        }
    }

    @Transactional
    public void clearRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));

        user.setRefreshToken(null);
        userRepository.save(user);
    }
}
