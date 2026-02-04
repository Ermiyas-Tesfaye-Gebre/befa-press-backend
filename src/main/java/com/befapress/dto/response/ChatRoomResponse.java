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
public class ChatRoomResponse {
    private Long id;
    private String type;
    private Long referenceId;
    private String referenceTitle; // Opinion title for OPINION_DISCUSSION type
    private String status;
    private LocalDateTime createdAt;
    private Long unreadCount; // Number of unread messages for current user
    private String lastMessagePreview; // Preview of latest message
    private String lastMessageSender; // Name of last message sender
    private LocalDateTime lastMessageAt; // Timestamp of last message
}
