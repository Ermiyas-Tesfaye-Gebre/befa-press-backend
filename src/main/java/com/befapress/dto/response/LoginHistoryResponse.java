package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
}
