package com.befapress.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User growth statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthDTO {
    private Long totalUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    private String changePercent;
    private String trend;
    private List<TrafficDataDTO> chartData;
}
