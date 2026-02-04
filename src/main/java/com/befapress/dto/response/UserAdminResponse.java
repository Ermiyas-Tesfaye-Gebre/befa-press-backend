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
public class UserAdminResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String bio;
    private String profilePic;
    private String affiliation;
    private String expertiseField;
    private boolean isVerified;
    private boolean isEmailVerified;
    private String status;
    private String role;
    private String departmentName;
    private Long departmentId;
    private int failedLoginAttempts;
    private LocalDateTime lockedAt;
    private Long createdBy;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
