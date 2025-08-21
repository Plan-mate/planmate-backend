package com.planmate.planmate_backend.auth.service;

import com.planmate.planmate_backend.common.config.AppProperties;
import com.planmate.planmate_backend.auth.dto.KakaoProfileDto;
import com.planmate.planmate_backend.common.exception.BusinessException;
import org.springframework.core.ParameterizedTypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    private String getClientId() {
        return appProperties.getKakao().getClientId();
    }

    private String getRedirectUri() {
        return appProperties.getKakao().getRedirectUri();
    }

    private String getAccessToken(String code) {
        String body = UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", getClientId())
                .queryParam("redirect_uri", getRedirectUri())
                .queryParam("code", code)
                .build()
                .toUriString()
                .substring(1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        Map<String, Object> responseBody = Objects.requireNonNull(restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody()
        );

        return (String) responseBody.get("access_token");
    }

    @SuppressWarnings("unchecked")
    public KakaoProfileDto getUserInfo(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken(code));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        Map<String, Object> kakaoMap = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        if (kakaoMap == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "카카오 프로필을 가져오지 못했습니다.");
        }

        Long id = null;
        String nickname = null;
        String profileImage = null;

        Object idObj = kakaoMap.get("id");
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        }

        Object propertiesObj = kakaoMap.get("properties");
        if (propertiesObj instanceof Map) {
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            nickname = (String) properties.get("nickname");
            profileImage = (String) properties.get("profile_image");
        }

        return new KakaoProfileDto(id, nickname, profileImage);
    }
}
