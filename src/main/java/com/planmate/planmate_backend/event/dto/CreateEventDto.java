package com.planmate.planmate_backend.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventDto {

    @NotBlank(message = "categoryId는 비어 있을 수 없습니다.")
    @Pattern(regexp = "^[0-9]+$", message = "categoryId는 숫자여야 합니다.")
    private String categoryId;

    private String description;

    @NotNull(message = "startTime은 필수 값입니다.")
    private LocalDateTime startTime;

    @NotNull(message = "endTime은 필수 값입니다.")
    private LocalDateTime endTime;

    @NotBlank(message = "title은 비어 있을 수 없습니다.")
    private String title;

    @NotNull(message = "isRecurring 값은 필수입니다.")
    private Boolean isRecurring;

    @Valid
    private CreateRecurrenceRuleDto recurrenceRule;
}
