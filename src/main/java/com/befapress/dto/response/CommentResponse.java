package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String authorName;
    private String authorProfilePic;
    private boolean isGuest;
    private String status;
    private int likeCount;
    private List<String> moderationFlags;
    private LocalDateTime createdAt;

    // Context info
    private Long opinionId;
    private String opinionTitle;
    private Long newsId;
    private String newsTitle;

    private List<CommentResponse> replies;
}
