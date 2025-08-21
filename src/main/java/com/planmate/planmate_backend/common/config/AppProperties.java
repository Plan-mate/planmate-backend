package com.planmate.planmate_backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Kakao kakao = new Kakao();

    @Getter
    @Setter
    public static class Jwt {
        private String accessSecret;
        private String refreshSecret;
        private long accessExpiration;
        private long refreshExpiration;
    }

    @Getter
    @Setter
    public static class Kakao {
        private String clientId;
        private String redirectUri;
    }
}

