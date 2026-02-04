package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String token;

    private String platform; // ANDROID, IOS, WEB

    private String deviceName;
}
