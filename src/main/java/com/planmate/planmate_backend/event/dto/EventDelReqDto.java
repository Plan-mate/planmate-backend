package com.planmate.planmate_backend.event.dto;

import com.planmate.planmate_backend.common.enums.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDelReqDto {
    private Long eventId;
    private Scope scope;
}
