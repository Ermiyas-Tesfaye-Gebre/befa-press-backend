package com.befapress.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRuleRequest {
    @NotBlank(message = "Category is required")
    private String category; // INSULT, HATE_SPEECH, etc.

    @NotBlank(message = "Language is required")
    private String language; // AMHARIC, ENGLISH

    @NotBlank(message = "Pattern is required")
    private String pattern; // The word or regex
}
