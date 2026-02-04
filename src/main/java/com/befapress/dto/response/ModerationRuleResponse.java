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
public class ModerationRuleResponse {
    private Long id;
    private String category;
    private String language;
    private String pattern;
    private LocalDateTime createdAt;
}
