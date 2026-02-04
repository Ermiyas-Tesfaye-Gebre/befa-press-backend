package com.befapress.controller;

import com.befapress.dto.request.ChatMessageRequest;
import com.befapress.dto.response.ChatMessageResponse;
import com.befapress.dto.response.ChatRoomResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.entity.ChatRoom;
import com.befapress.entity.User;
import com.befapress.repository.UserRepository;
import com.befapress.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat and Messaging APIs")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping("/opinion/{opinionId}")
    @Operation(summary = "Get or create a chat room for an opinion")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getOpinionChat(
            @PathVariable Long opinionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("getOpinionChat called for opinionId: {}", opinionId);
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ChatRoom room = chatService.getOpinionChatRoom(opinionId);

            if (room == null) {
                room = chatService.createOpinionChatRoom(opinionId, user);
            } else {
                chatService.addParticipant(room, user, com.befapress.entity.ChatParticipant.Role.MEMBER);
            }

            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("Error in getOpinionChat: {} - {}", e.getClass().getName(), e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get current user's chat rooms")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getMyRooms(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("getMyRooms called for user: {}", userDetails != null ? userDetails.getUsername() : "NULL");

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<ChatRoomResponse> result = chatService.getUserRooms(user, PageRequest.of(page, size));
            log.info("getMyRooms - Returning {} rooms", result.getTotalElements());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in getMyRooms: {} - {}", e.getClass().getName(), e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Get message history")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ChatMessageResponse>> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(chatService.getMessages(roomId, pageable));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message (REST fallback)")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("sendMessage called. Content length: {}",
                request.getContent() != null ? request.getContent().length() : 0);
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatMessageResponse response = chatService.sendMessage(request, user);
        log.info("Message sent successfully. ID: {}", response.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/read")
    @Operation(summary = "Mark all messages in a room as read")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("=== markAsRead CONTROLLER === roomId={}, user={}", roomId,
                    userDetails != null ? userDetails.getUsername() : "NULL");

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User found: id={}, email={}", user.getId(), user.getEmail());

            chatService.markMessagesAsRead(roomId, user.getId());

            log.info("markAsRead completed successfully for roomId={}", roomId);
            return ResponseEntity.ok(new MessageResponse("Messages marked as read", true));
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get total unread message count for current user")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getTotalUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Long totalUnread = chatService.getTotalUnreadCount(user);
            return ResponseEntity.ok(java.util.Map.of("unreadCount", totalUnread));
        } catch (Exception e) {
            log.error("Error getting unread count: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file for chat")
    @PreAuthorize("hasAnyRole('INTELLECTUAL', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadChatFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();

            // Allowed types
            String[] allowedTypes = {
                    // Images
                    "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp",
                    // PDF
                    "application/pdf",
                    // Word
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    // Excel
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    // Text
                    "text/plain"
            };

            boolean isAllowed = false;
            for (String type : allowedTypes) {
                if (type.equalsIgnoreCase(contentType)) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "error", "File type not supported. Allowed: Images, PDF, Word, Excel, Text files"));
            }

            // Max 10MB
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "error", "File too large. Maximum size is 10MB"));
            }

            String filePath = chatService.uploadChatFile(file);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("url", filePath);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("mimeType", contentType);
            response.put("isImage", contentType != null && contentType.startsWith("image/"));

            log.info("Chat file uploaded: {}", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading chat file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
