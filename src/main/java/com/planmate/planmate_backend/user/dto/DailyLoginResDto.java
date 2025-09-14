package com.planmate.planmate_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DailyLoginResDto {
    private boolean isFirstLoginToday;
}
