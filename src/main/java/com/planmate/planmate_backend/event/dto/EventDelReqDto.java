package com.planmate.planmate_backend.event.dto;

import com.planmate.planmate_backend.common.enums.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDelReqDto {
    private Scope scope;
    private LocalDateTime targetTime;

}
