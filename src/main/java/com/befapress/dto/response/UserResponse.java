package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String bio;
    private String profilePic;
    private String affiliation;
    private String expertiseField;
    private boolean isVerified;
    private boolean isEmailVerified;
    private String status;
    private String role;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
