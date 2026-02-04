package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String bio;

    private String affiliation;

    private String expertiseField;

    private String profilePic;
}
