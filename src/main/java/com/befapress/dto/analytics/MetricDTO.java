package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single metric with value, change percentage, and trend direction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
    private String title;
    private String value;
    private String change; // e.g., "+12.5%" or "-2.4%"
    private String trend; // "up" or "down"
    private String color; // for UI styling
}
