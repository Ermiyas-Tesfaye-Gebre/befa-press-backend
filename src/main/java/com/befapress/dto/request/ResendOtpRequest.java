package com.befapress.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;
}
