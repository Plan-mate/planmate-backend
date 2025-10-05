package com.planmate.planmate_backend.summary.service;

import com.planmate.planmate_backend.event.service.GetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalStatAnalyzer {

    private final GetService getService;

    private static final double ALPHA = 1.0; // 최근 smoothing
    private static final double BETA  = 1.0; // 과거 smoothing
    private static final long   MIN_RECENT = 0L; // 최소 카운트 필터링

    public Map<Long, Double> computeHotScores(LocalDateTime now, int windowDays) {
        LocalDateTime recentStart = now.minusDays(windowDays);
        LocalDateTime pastStart   = now.minusDays(windowDays * 2L);

        Map<Long, Long> recent = getService.countCategoryBetween(recentStart, now);
        Map<Long, Long> past   = getService.countCategoryBetween(pastStart, recentStart);

        Map<Long, Double> scores = new HashMap<>();
        for (Map.Entry<Long, Long> e : recent.entrySet()) {
            Long categoryId = e.getKey();
            long recentCnt  = e.getValue();
            if (recentCnt < MIN_RECENT) continue;

            long pastCnt = past.getOrDefault(categoryId, 0L);
            double score = (recentCnt + ALPHA) / (pastCnt + BETA);
            scores.put(categoryId, score);
        }
        return scores;
    }

    public List<Long> getHotCategories(LocalDateTime now, int windowDays, int topN) {
        Map<Long, Double> scores = computeHotScores(now, windowDays);
        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Optional<Long> getHotCategory(LocalDateTime now, int windowDays) {
        List<Long> top = getHotCategories(now, windowDays, 1);
        return top.isEmpty() ? Optional.empty() : Optional.of(top.get(0));
    }

    public List<Long> getRecentTopCategories(LocalDateTime now, int windowDays, int topN) {
        LocalDateTime recentStart = now.minusDays(windowDays);
        Map<Long, Long> recent = getService.countCategoryBetween(recentStart, now);
        return recent.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
