package com.planmate.planmate_backend.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planmate.planmate_backend.common.config.AppProperties;
import com.planmate.planmate_backend.summary.dto.HourlyWeatherDto;
import com.planmate.planmate_backend.summary.dto.WeatherSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getApiKey() {
        return appProperties.getWeather().getApiKey();
    }

    private String getUltraBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        if (minute >= 45) return String.format("%02d30", hour);
        else return String.format("%02d30", hour == 0 ? 0 : hour - 1);
    }

    private String getVilageBaseTime() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 2) return "2300";
        else if (hour < 5) return "0200";
        else if (hour < 8) return "0500";
        else if (hour < 11) return "0800";
        else if (hour < 14) return "1100";
        else if (hour < 17) return "1400";
        else if (hour < 20) return "1700";
        else if (hour < 23) return "2000";
        else return "2300";
    }

    private String getWeatherDescription(String pty, String sky) {
        if ("0".equals(pty)) {
            return switch (sky) {
                case "1" -> "맑음";
                case "3" -> "구름 많음";
                case "4" -> "흐림";
                default -> "알 수 없음";
            };
        } else {
            return switch (pty) {
                case "1" -> "비";
                case "2" -> "비/눈";
                case "3" -> "눈";
                case "5" -> "빗방울";
                case "6" -> "빗방울/눈날림";
                case "7" -> "눈날림";
                default -> "알 수 없음";
            };
        }
    }

    private String makeDailyComment(String sky, boolean rainExpected) {
        if (rainExpected) return "오늘은 비가 내릴 가능성이 있습니다. 우산을 챙기세요.";
        return switch (sky) {
            case "맑음" -> "하루 종일 맑은 날씨가 이어질 전망입니다.";
            case "구름 많음" -> "대체로 구름이 많겠지만 큰 비 소식은 없습니다.";
            case "흐림" -> "하루 종일 흐린 하늘이 이어지겠습니다.";
            default -> "";
        };
    }

    private LocalDateTime floorTo3Hour(LocalDateTime dt) {
        int h = dt.getHour();
        int floored = (h / 3) * 3;
        return dt.withHour(floored).withMinute(0).withSecond(0).withNano(0);
    }

    private double calculateFeelsLike(Double tempC, Double windMs, Double rh) {
        if (tempC == null) return Double.NaN;
        double t = tempC;
        double vMs = windMs == null ? 0.0 : windMs;
        double vKmh = vMs * 3.6;
        double humidity = rh == null ? 50.0 : rh;

        if (t <= 10.0 && vKmh >= 4.8) {
            return 13.12 + 0.6215 * t - 11.37 * Math.pow(vKmh, 0.16) + 0.3965 * t * Math.pow(vKmh, 0.16);
        }
        if (t >= 27.0 && humidity >= 40.0) {
            double T = t * 9/5 + 32;
            double HI = -42.379 + 2.04901523*T + 10.14333127*humidity
                    - 0.22475541*T*humidity - 6.83783e-3*T*T - 5.481717e-2*humidity*humidity
                    + 1.22874e-3*T*T*humidity + 8.5282e-4*T*humidity*humidity - 1.99e-6*T*T*humidity*humidity;
            return (HI - 32) * 5/9;
        }
        return t;
    }

    private Integer toIntOrNull(String s) {
        try { return (s == null || s.isEmpty()) ? null : Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private Double toDoubleOrNull(String s) {
        try { return (s == null || s.isEmpty() || s.equals("-999")) ? null : Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }

    public WeatherSummaryDto getWeatherSummary(int nx, int ny, String locationName) {
        try {
            LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            LocalDateTime start = now.plusHours(1);
            List<LocalDateTime> timeline = new ArrayList<>();
            for (int i = 0; i < 12; i++) timeline.add(start.plusHours(i));

            LocalDate today = LocalDate.now();
            String baseDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String ultraUrl = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apihub.kma.go.kr")
                    .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtFcst")
                    .queryParam("authKey", getApiKey())
                    .queryParam("numOfRows", "200")
                    .queryParam("pageNo", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", getUltraBaseTime())
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .toUriString();

            JsonNode ultraItems = objectMapper.readTree(restTemplate.getForObject(ultraUrl, String.class))
                    .path("response").path("body").path("items").path("item");

            Map<LocalDateTime, Map<String, String>> ultraMap = new HashMap<>();
            ultraItems.forEach(item -> {
                String cat = item.path("category").asText();
                if (!List.of("PTY", "SKY", "T1H").contains(cat)) return;
                String fcstDate = item.path("fcstDate").asText(); // yyyyMMdd
                String fcstTime = item.path("fcstTime").asText(); // HHmm
                int h = Integer.parseInt(fcstTime.substring(0, 2));
                int m = Integer.parseInt(fcstTime.substring(2, 4));
                LocalDate date = LocalDate.parse(fcstDate, DateTimeFormatter.BASIC_ISO_DATE);
                LocalDateTime dt = date.atTime(h, m, 0, 0);
                ultraMap.putIfAbsent(dt, new HashMap<>());
                ultraMap.get(dt).put(cat, item.path("fcstValue").asText());
            });

            String vilageUrl = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apihub.kma.go.kr")
                    .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                    .queryParam("authKey", getApiKey())
                    .queryParam("numOfRows", "500")
                    .queryParam("pageNo", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", getVilageBaseTime())
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .toUriString();

            JsonNode vilageItems = objectMapper.readTree(restTemplate.getForObject(vilageUrl, String.class))
                    .path("response").path("body").path("items").path("item");

            Map<LocalDateTime, Map<String, String>> vilageMap = new HashMap<>();
            Integer tmx = null, tmn = null;
            String todaySky = "맑음";
            String wctOfficial = null;

            for (JsonNode item : vilageItems) {
                String cat = item.path("category").asText();
                String val = item.path("fcstValue").asText();

                switch (cat) {
                    case "TMX" -> { Integer v = toIntOrNull(val); if (v != null) tmx = (tmx == null) ? v : Math.max(tmx, v); }
                    case "TMN" -> { Integer v = toIntOrNull(val); if (v != null) tmn = (tmn == null) ? v : Math.min(tmn, v); }
                    case "WCT" -> { wctOfficial = val; }
                }

                if (List.of("TMP", "PTY", "SKY", "REH", "WSD").contains(cat)) {
                    String fcstDate = item.path("fcstDate").asText();
                    String fcstTime = item.path("fcstTime").asText();
                    int h = Integer.parseInt(fcstTime.substring(0, 2));
                    int m = Integer.parseInt(fcstTime.substring(2, 4));
                    LocalDate date = LocalDate.parse(fcstDate, DateTimeFormatter.BASIC_ISO_DATE);
                    LocalDateTime dt = date.atTime(h, m, 0, 0);
                    vilageMap.putIfAbsent(dt, new HashMap<>());
                    vilageMap.get(dt).put(cat, val);

                    if ("SKY".equals(cat)) {
                        todaySky = getWeatherDescription("0", val);
                    }
                }
            }

            List<HourlyWeatherDto> hourly = new ArrayList<>();
            boolean rainExpected = false;

            Double feelsLike = null;
            for (int i = 0; i < timeline.size(); i++) {
                LocalDateTime slot = timeline.get(i);

                Map<String, String> ultra = ultraMap.getOrDefault(slot, Collections.emptyMap());

                LocalDateTime ref3h = floorTo3Hour(slot);
                Map<String, String> v = vilageMap.getOrDefault(ref3h, Collections.emptyMap());

                String pty = ultra.getOrDefault("PTY", v.getOrDefault("PTY", "0"));
                String sky = ultra.getOrDefault("SKY", v.getOrDefault("SKY", "1"));
                String tStr = ultra.getOrDefault("T1H", v.get("TMP"));

                Integer temp = toIntOrNull(tStr);
                if (temp == null) temp = 0;

                String desc = getWeatherDescription(pty, sky);
                if (!"0".equals(pty)) rainExpected = true;

                if (i == 0) {
                    if (wctOfficial != null && !wctOfficial.isEmpty()) {
                        Double w = toDoubleOrNull(wctOfficial);
                        feelsLike = w;
                    } else {
                        Double rh = toDoubleOrNull(v.get("REH"));
                        Double wsd = toDoubleOrNull(v.get("WSD"));
                        feelsLike = calculateFeelsLike(temp.doubleValue(), wsd, rh);
                    }
                }

                String label = String.format("%02d시", slot.getHour());
                hourly.add(new HourlyWeatherDto(label, desc, temp));
            }

            if (hourly.isEmpty()) {
                return new WeatherSummaryDto("알 수 없음", "날씨 데이터를 불러오지 못했습니다.", Collections.emptyList());
            }

            int maxT = (tmx != null) ? tmx : hourly.stream().mapToInt(HourlyWeatherDto::getTemperature).max().orElse(hourly.get(0).getTemperature());
            int minT = (tmn != null) ? tmn : hourly.stream().mapToInt(HourlyWeatherDto::getTemperature).min().orElse(hourly.get(0).getTemperature());

            String feelsText = (feelsLike == null || feelsLike.isNaN()) ? "자료 없음" : String.valueOf(Math.round(feelsLike));

            String summary = String.format(
                    "%s 지역 오늘 날씨는 %s입니다. 현재 기온 %d℃, 체감온도 %s℃, 최고 %d℃ / 최저 %d℃ 예상. %s",
                    locationName,
                    todaySky,
                    hourly.get(0).getTemperature(),
                    feelsText,
                    maxT,
                    minT,
                    makeDailyComment(todaySky, rainExpected)
            );

            return new WeatherSummaryDto(todaySky, summary, hourly);

        } catch (Exception e) {
            e.printStackTrace();
            return new WeatherSummaryDto("알 수 없음", "날씨 데이터를 불러오지 못했습니다.", Collections.emptyList());
        }
    }
}
