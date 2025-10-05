package com.planmate.planmate_backend.summary.loader;

import com.planmate.planmate_backend.summary.dto.WeatherBasicInfo;

public class WeatherKeyBuilder {

    public static String build(WeatherBasicInfo weather) {
        boolean rain = weather.getForecastMap().values().stream()
                .anyMatch(m -> !"0".equals(m.getOrDefault("PTY", "0")));
        if (rain) return "RAIN";

        String sky = weather.getTodaySky();
        Integer maxT = weather.getTmx();
        Integer minT = weather.getTmn();

        if ("맑음".equals(sky)) {
            if (maxT != null && maxT >= 28) return "SUNNY_HOT";
            return "SUNNY_COOL";
        }

        if ("흐림".equals(sky) || "구름 많음".equals(sky)) return "CLOUDY";
        if (minT != null && minT <= 8) return "COLD";

        return "DEFAULT";
    }
}
