package com.planmate.planmate_backend.event.dto;

import com.planmate.planmate_backend.common.enums.Frequency;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecurrenceRuleDto {

    private List<Integer> daysOfMonth;

    private List<String> daysOfWeek;

    @NotNull(message = "frequency는 필수 값입니다.")
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Min(value = 1, message = "interval은 1 이상이어야 합니다.")
    private Integer interval;

    private LocalDateTime endDate;
}
