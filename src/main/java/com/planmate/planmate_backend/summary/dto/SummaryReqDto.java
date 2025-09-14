package com.planmate.planmate_backend.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryReqDto {
    private int nx;
    private int ny;
    private String locationName;
}
