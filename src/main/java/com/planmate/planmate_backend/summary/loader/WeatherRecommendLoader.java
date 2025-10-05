package com.planmate.planmate_backend.summary.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planmate.planmate_backend.summary.dto.RecommendTemplate;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WeatherRecommendLoader {

    private final ObjectMapper objectMapper;
    private Map<String, List<RecommendTemplate>> recommendations = Collections.emptyMap();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource("weather_recommendations.json");
            recommendations = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<Map<String, List<RecommendTemplate>>>() {}
            );
        } catch (Exception e) {
            recommendations = Collections.emptyMap();
        }
    }

    public List<RecommendTemplate> getRecommendations(String key) {
        return recommendations.getOrDefault(
                key,
                recommendations.getOrDefault("DEFAULT", Collections.emptyList())
        );
    }
}
