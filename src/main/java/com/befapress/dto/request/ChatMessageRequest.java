package com.befapress.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String content;
    private Long roomId;
    // For creating a room implicitly (e.g., first message on an issue)
    private Long opinionId;
    private String type; // TEXT, IMAGE, FILE

    // File attachment fields
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMimeType;
}
