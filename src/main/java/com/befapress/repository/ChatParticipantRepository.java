package com.befapress.repository;

import com.befapress.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.room.id = :roomId AND cp.user.id = :userId")
    Optional<ChatParticipant> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.user.id = :userId")
    List<ChatParticipant> findAllByUserId(@Param("userId") Long userId);
}
