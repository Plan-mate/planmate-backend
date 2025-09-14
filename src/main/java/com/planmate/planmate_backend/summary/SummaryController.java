package com.planmate.planmate_backend.summary;

import com.planmate.planmate_backend.summary.dto.SummaryReqDto;
import com.planmate.planmate_backend.summary.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/today")
    public String getTodaySummary(@Valid SummaryReqDto dto) {
        return summaryService.getTodaySummary(dto);
    }

    // 추천 API
    @GetMapping("/recommend")
    public void getRecommendations(@AuthenticationPrincipal Long userId) {
        System.out.println(userId);
    }

}