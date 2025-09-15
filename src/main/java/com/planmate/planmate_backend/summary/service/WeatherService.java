package com.planmate.planmate_backend.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planmate.planmate_backend.common.config.AppProperties;
import com.planmate.planmate_backend.summary.dto.WeatherSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getApiKey() {
        return appProperties.getWeather().getApiKey();
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
                case "4" -> "소나기";
                default -> "알 수 없음";
            };
        }
    }

    public WeatherSummaryDto getWeatherSummary(int nx, int ny, String locationName) {
        try {
            LocalDate now = LocalDate.now();
            String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String formattedTime = String.format("%02d00",
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY));

            String ncstUrl = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apis.data.go.kr")
                    .path("/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
                    .queryParam("serviceKey", getApiKey())
                    .queryParam("numOfRows", "10")
                    .queryParam("pageNo", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", formattedDate)
                    .queryParam("base_time", formattedTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUriString();

            JsonNode ncstNode = objectMapper.readTree(restTemplate.getForObject(ncstUrl, String.class))
                    .path("response").path("body").path("items").path("item");
            List<JsonNode> ncstItems = new ArrayList<>();
            ncstNode.forEach(ncstItems::add);

            String temp = findItemValue(ncstItems, "T1H");
            String pty = findItemValue(ncstItems, "PTY");

            String fcstUrl = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apis.data.go.kr")
                    .path("/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst")
                    .queryParam("serviceKey", getApiKey())
                    .queryParam("numOfRows", "60")
                    .queryParam("pageNo", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", formattedDate)
                    .queryParam("base_time", formattedTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUriString();

            JsonNode fcstNode = objectMapper.readTree(restTemplate.getForObject(fcstUrl, String.class))
                    .path("response").path("body").path("items").path("item");
            List<JsonNode> fcstItems = new ArrayList<>();
            fcstNode.forEach(fcstItems::add);

            String sky = getWeatherDescription(pty, findItemValue(fcstItems, "SKY"));

            String vilageFcstUrl = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apis.data.go.kr")
                    .path("/1360000/VilageFcstInfoService_2.0/getVilageFcst")
                    .queryParam("serviceKey", getApiKey())
                    .queryParam("numOfRows", "1000")
                    .queryParam("pageNo", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", formattedDate)
                    .queryParam("base_time", "0500")
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUriString();

            JsonNode vilageNode = objectMapper.readTree(restTemplate.getForObject(vilageFcstUrl, String.class))
                    .path("response").path("body").path("items").path("item");
            List<JsonNode> vilageItems = new ArrayList<>();
            vilageNode.forEach(vilageItems::add);

            double maxTemp = vilageItems.stream()
                    .filter(item -> "TMX".equals(item.path("category").asText()))
                    .mapToDouble(item -> parseDoubleSafe(item.path("fcstValue").asText(), Double.NEGATIVE_INFINITY))
                    .max().orElse(Double.NaN);

            double minTemp = vilageItems.stream()
                    .filter(item -> "TMN".equals(item.path("category").asText()))
                    .mapToDouble(item -> parseDoubleSafe(item.path("fcstValue").asText(), Double.POSITIVE_INFINITY))
                    .min().orElse(Double.NaN);

            int currentHour = LocalDateTime.now().getHour();
            List<Integer> rainHours = vilageItems.stream()
                    .filter(item -> "PTY".equals(item.path("category").asText()) && !"0".equals(item.path("fcstValue").asText()))
                    .map(item -> Integer.parseInt(item.path("fcstTime").asText().substring(0, 2)))
                    .filter(hour -> hour >= currentHour)
                    .sorted()
                    .toList();

            String rainTimeText = formatRainTimes(rainHours);

            String message = String.format(
                    "%s의 현재 날씨는 %s이고, 온도는 %s℃입니다. 오늘 날씨는 최고기온 %.1f℃, 최저기온 %.1f℃이며. %s",
                    locationName, sky, temp, maxTemp, minTemp, rainTimeText);

            return new WeatherSummaryDto(sky, message);

        } catch (Exception e) {
            e.printStackTrace();
            return new WeatherSummaryDto("알 수 없음", "날씨 정보를 불러오는데 실패했습니다.");
        }
    }

    private String findItemValue(List<JsonNode> items, String category) {
        return items.stream()
                .filter(item -> category.equals(item.path("category").asText()))
                .map(item -> {
                    String fcstValue = item.path("fcstValue").asText();
                    return fcstValue.isEmpty() ? item.path("obsrValue").asText() : fcstValue;
                })
                .findFirst().orElse("?");
    }

    private double parseDoubleSafe(String val, double defaultVal) {
        try { return Double.parseDouble(val); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private String formatRainTimes(List<Integer> hours) {
        if (hours.isEmpty()) return "오늘은 강수가 없습니다.";

        List<String> ranges = new ArrayList<>();
        int start = hours.get(0);
        int prev = start;

        for (int i = 1; i < hours.size(); i++) {
            int current = hours.get(i);
            if (current == prev + 1) {
                prev = current;
            } else {
                ranges.add(formatRange(start, prev));
                start = current;
                prev = current;
            }
        }
        ranges.add(formatRange(start, prev));

        if (ranges.size() == 1) {
            return "오늘은 " + ranges.get(0) + " 비 예보가 있습니다.";
        } else if (ranges.size() <= 3) {
            return "오늘은 " + String.join(", ", ranges) + " 비 예보가 있습니다.";
        } else {
            return "오늘은 산발적으로 비가 예보되어 있습니다.";
        }
    }

    private String formatRange(int start, int end) {
        if (start == end) return start + "시";
        return start + "시부터 " + end + "시까지";
    }
}
