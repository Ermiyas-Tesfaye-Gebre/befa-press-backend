package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Traffic data point for charts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficDataDTO {
    private LocalDate date;
    private String label; // "Jan", "Feb" or "2026-01-12"
    private Long views;
    private Long uniqueUsers;
    private Integer month; // for monthly data
}
