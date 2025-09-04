package com.planmate.planmate_backend.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DateDto {

    @NotNull(message = "연도는 필수입니다")
    @Min(value = 2000, message = "연도는 2000 이상이어야 합니다")
    @Max(value = 2100, message = "연도는 2100 이하이어야 합니다")
    private Integer year;

    @NotNull(message = "월은 필수입니다")
    @Min(value = 1, message = "월은 1 이상이어야 합니다")
    @Max(value = 12, message = "월은 12 이하이어야 합니다")
    private Integer month;
}
