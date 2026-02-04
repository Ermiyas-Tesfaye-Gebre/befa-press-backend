package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusRequest {

    @NotBlank(message = "Status is required")
    private String status; // ACTIVE, SUSPENDED, LOCKED, DEACTIVATED

    private String reason; // Optional reason for status change
}
