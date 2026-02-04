package com.befapress.repository;

import com.befapress.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByTypeAndReferenceId(ChatRoom.RoomType type, Long referenceId);
}
