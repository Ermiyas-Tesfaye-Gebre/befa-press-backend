package com.befapress.service;

import com.befapress.dto.request.ChatMessageRequest;
import com.befapress.dto.response.ChatMessageResponse;
import com.befapress.dto.response.ChatRoomResponse;
import com.befapress.entity.*;
import com.befapress.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final OpinionRepository opinionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ChatRoom getOpinionChatRoom(Long opinionId) {
        return roomRepository.findByTypeAndReferenceId(ChatRoom.RoomType.OPINION_DISCUSSION, opinionId)
                .orElse(null);
    }

    @Transactional
    public ChatRoom createOpinionChatRoom(Long opinionId, User initiator) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new RuntimeException("Opinion not found"));

        // Create Room
        ChatRoom room = ChatRoom.builder()
                .type(ChatRoom.RoomType.OPINION_DISCUSSION)
                .referenceId(opinionId)
                .status(ChatRoom.RoomStatus.ACTIVE)
                .build();
        room = roomRepository.save(room);

        // Add Author as Participant
        addParticipant(room, opinion.getAuthor(), ChatParticipant.Role.OWNER);

        // If initiator is an Admin and not the author, add them too
        if (!initiator.getId().equals(opinion.getAuthor().getId())) {
            addParticipant(room, initiator, ChatParticipant.Role.MEMBER);
        }

        log.info("Created chat room for Opinion ID: {}", opinionId);
        return room;
    }

    @Transactional
    public void addParticipant(ChatRoom room, User user, ChatParticipant.Role role) {
        if (participantRepository.findByRoomIdAndUserId(room.getId(), user.getId()).isPresent()) {
            return;
        }
        ChatParticipant participant = ChatParticipant.builder()
                .room(room)
                .user(user)
                .role(role)
                .build();
        participantRepository.save(participant);
    }

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, User sender) {
        ChatRoom room;

        // Handle implicit room creation for Opinion chats
        if (request.getRoomId() == null && request.getOpinionId() != null) {
            room = getOpinionChatRoom(request.getOpinionId());
            if (room == null) {
                room = createOpinionChatRoom(request.getOpinionId(), sender);
            }
        } else {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
        }

        // AUTO-HEALING: If this is an Opinion Chat, ensure the Opinion Author is ALWAYS
        // a participant.
        // This fixes the issue where Admin starts chat but Author isn't added or was
        // missed.
        if (room.getType() == ChatRoom.RoomType.OPINION_DISCUSSION && room.getReferenceId() != null) {
            Opinion opinion = opinionRepository.findById(room.getReferenceId()).orElse(null);
            if (opinion != null && opinion.getAuthor() != null) {
                if (participantRepository.findByRoomIdAndUserId(room.getId(), opinion.getAuthor().getId()).isEmpty()) {
                    log.info("Auto-adding Author {} to Room {}", opinion.getAuthor().getEmail(), room.getId());
                    addParticipant(room, opinion.getAuthor(), ChatParticipant.Role.OWNER);
                }
            }
        }

        // Ensure sender is a participant
        addParticipant(room, sender, ChatParticipant.Role.MEMBER);

        // Build metadata JSON for file attachments
        String metadata = null;
        if (request.getFileUrl() != null && !request.getFileUrl().isEmpty()) {
            try {
                java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
                metaMap.put("fileUrl", request.getFileUrl());
                metaMap.put("fileName", request.getFileName());
                metaMap.put("fileSize", request.getFileSize());
                metaMap.put("fileMimeType", request.getFileMimeType());
                metadata = objectMapper.writeValueAsString(metaMap);
            } catch (Exception e) {
                log.error("Failed to serialize file metadata", e);
            }
        }

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .type(ChatMessage.MessageType.valueOf(request.getType() != null ? request.getType() : "TEXT"))
                .status(ChatMessage.MessageStatus.SENT)
                .metadata(metadata)
                .build();

        message = messageRepository.save(message);
        log.info("Message saved. ID: {}, type: {}", message.getId(), message.getType());

        ChatMessageResponse response = mapToResponse(message);

        // Broadcast to WebSocket
        messagingTemplate.convertAndSend("/topic/chat.room." + room.getId(), response);

        // Notify participants (optional - handled by client subscription mostly)
        // If we had a specific notification channel per user, we'd send it here

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getUserRooms(User user, Pageable pageable) {
        log.info("getUserRooms: Looking for rooms for user ID: {}, email: {}", user.getId(), user.getEmail());

        java.util.List<ChatParticipant> participants = participantRepository.findAllByUserId(user.getId());
        log.info("getUserRooms: Found {} participant entries", participants.size());

        java.util.List<ChatRoomResponse> roomResponses = participants.stream()
                .map(ChatParticipant::getRoom)
                .distinct()
                .map(room -> mapToRoomResponse(room, user.getId()))
                .sorted((a, b) -> {
                    // Sort by last message time (most recent first), then by unread count
                    if (a.getLastMessageAt() != null && b.getLastMessageAt() != null) {
                        return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("getUserRooms: Returning {} distinct rooms", roomResponses.size());

        return new org.springframework.data.domain.PageImpl<>(roomResponses, pageable, roomResponses.size());
    }

    private ChatRoomResponse mapToRoomResponse(ChatRoom room, Long userId) {
        String title = null;
        if (room.getType() == ChatRoom.RoomType.OPINION_DISCUSSION && room.getReferenceId() != null) {
            title = opinionRepository.findById(room.getReferenceId())
                    .map(Opinion::getTitle)
                    .orElse("Opinion #" + room.getReferenceId());
        }

        // Get unread count for this user
        Long unreadCount = messageRepository.countUnreadByRoomIdAndUserId(room.getId(), userId);

        // Get latest message for preview
        ChatMessage lastMessage = messageRepository.findLatestByRoomId(room.getId());
        String lastMessagePreview = null;
        String lastMessageSender = null;
        java.time.LocalDateTime lastMessageAt = null;

        if (lastMessage != null) {
            lastMessagePreview = lastMessage.getContent();
            if (lastMessagePreview != null && lastMessagePreview.length() > 50) {
                lastMessagePreview = lastMessagePreview.substring(0, 50) + "...";
            }
            lastMessageSender = lastMessage.getSender().getFullName();
            lastMessageAt = lastMessage.getCreatedAt();
        }

        return ChatRoomResponse.builder()
                .id(room.getId())
                .type(room.getType().name())
                .referenceId(room.getReferenceId())
                .referenceTitle(title)
                .status(room.getStatus().name())
                .createdAt(room.getCreatedAt())
                .unreadCount(unreadCount)
                .lastMessagePreview(lastMessagePreview)
                .lastMessageSender(lastMessageSender)
                .lastMessageAt(lastMessageAt)
                .build();
    }

    private ChatMessageResponse mapToResponse(ChatMessage msg) {
        ChatMessageResponse.ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getRoom().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFullName())
                .content(msg.getContent())
                .type(msg.getType().name())
                .status(msg.getStatus().name())
                .createdAt(msg.getCreatedAt());

        // Parse file metadata if present
        if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
            try {
                java.util.Map<String, Object> metadata = objectMapper.readValue(
                        msg.getMetadata(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
                builder.fileUrl((String) metadata.get("fileUrl"));
                builder.fileName((String) metadata.get("fileName"));
                if (metadata.get("fileSize") != null) {
                    builder.fileSize(((Number) metadata.get("fileSize")).longValue());
                }
                builder.fileMimeType((String) metadata.get("fileMimeType"));
            } catch (Exception e) {
                log.warn("Failed to parse message metadata: {}", e.getMessage());
            }
        }

        return builder.build();
    }

    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        log.info("=== markMessagesAsRead START === roomId={}, userId={}", roomId, userId);

        try {
            // Find all unread messages not sent by user and mark them as read
            java.util.List<ChatMessage> unreadMessages = messageRepository.findUnreadByRoomAndNotSender(
                    roomId, userId);

            log.info("Found {} unread messages to mark as read", unreadMessages.size());

            if (unreadMessages != null && !unreadMessages.isEmpty()) {
                for (ChatMessage msg : unreadMessages) {
                    log.info("  Marking message ID={} as READ", msg.getId());
                    msg.setStatus(ChatMessage.MessageStatus.READ);
                }
                messageRepository.saveAll(unreadMessages);
                log.info("Saved {} messages as READ", unreadMessages.size());
            }

            log.info("=== markMessagesAsRead END === Success for room {} user {}", roomId, userId);
        } catch (Exception e) {
            log.error("Error in markMessagesAsRead: {}", e.getMessage(), e);
            throw e; // Re-throw so controller knows it failed
        }
    }

    @Transactional(readOnly = true)
    public Long getTotalUnreadCount(User user) {
        java.util.List<ChatParticipant> participants = participantRepository.findAllByUserId(user.getId());
        return participants.stream()
                .map(p -> messageRepository.countUnreadByRoomIdAndUserId(p.getRoom().getId(), user.getId()))
                .reduce(0L, Long::sum);
    }

    public String uploadChatFile(MultipartFile file) {
        return fileStorageService.storeFile(file, "chat");
    }
}
