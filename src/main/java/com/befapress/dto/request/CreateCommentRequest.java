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
public class CreateCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String content;

    // For guest comments
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String guestName;

    @Email(message = "Invalid email format")
    private String guestEmail;

    // For replies
    private Long parentId;
}
