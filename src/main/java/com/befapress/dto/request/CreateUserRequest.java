package com.befapress.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be less than 150 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private String role; // ROLE_ADMIN, ROLE_EDITOR, etc.

    private Long departmentId;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    @Size(max = 255, message = "Affiliation must be less than 255 characters")
    private String affiliation;

    @Size(max = 255, message = "Expertise field must be less than 255 characters")
    private String expertiseField;
}
