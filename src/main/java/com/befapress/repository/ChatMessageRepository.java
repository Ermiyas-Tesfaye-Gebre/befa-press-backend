package com.befapress.repository;

import com.befapress.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
        Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

        // Count unread messages in a room for a user (messages not sent by them and
        // status is SENT or DELIVERED)
        @Query(value = "SELECT COUNT(*) FROM chat_messages WHERE room_id = :roomId AND sender_id <> :userId AND status IN ('SENT', 'DELIVERED')", nativeQuery = true)
        Long countUnreadByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

        // Get latest message in a room (for preview)
        @Query(value = "SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
        ChatMessage findLatestByRoomId(@Param("roomId") Long roomId);

        // Find unread messages in a room not sent by user (status is SENT or DELIVERED)
        @Query(value = "SELECT * FROM chat_messages WHERE room_id = :roomId AND sender_id <> :userId AND status IN ('SENT', 'DELIVERED')", nativeQuery = true)
        List<ChatMessage> findUnreadByRoomAndNotSender(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
