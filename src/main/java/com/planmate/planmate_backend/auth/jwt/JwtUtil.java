package com.planmate.planmate_backend.auth.jwt;

import com.planmate.planmate_backend.common.config.AppProperties;
import com.planmate.planmate_backend.common.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final AppProperties appProperties;

    private Key getAccessKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getAccessSecret().getBytes());
    }

    private Key getRefreshKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getRefreshSecret().getBytes());
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Date expiry = Date.from(now.plusSeconds(appProperties.getJwt().getAccessExpiration()));

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(expiry)
                .signWith(getAccessKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Date expiry = Date.from(now.plusSeconds(appProperties.getJwt().getRefreshExpiration()));

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(expiry)
                .signWith(getRefreshKey())
                .compact();
    }

    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getAccessKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    public Claims validateRefreshToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getRefreshKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "리프레쉬 토큰이 만료되었습니다.");
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "잘못된 JWT 형식입니다.");
        } catch (SecurityException e) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "JWT 서명 검증 실패");
        }
    }

}
