package com.befapress.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private String type;
    private String status;
    private LocalDateTime createdAt;

    // File attachment fields
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMimeType;
}
