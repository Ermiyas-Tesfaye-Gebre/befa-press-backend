package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String nameAmharic;
    private String slug;
    private String description;
    private String icon;
    private String status;
    private int displayOrder;
    private long newsCount;
}
