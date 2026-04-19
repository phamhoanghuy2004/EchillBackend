package com.echill.repository;

import com.echill.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Lấy tin nhắn theo conversation, sắp xếp theo thời gian giảm dần (mới nhất trước),
     * fetch sender để tránh N+1.
     */
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            WHERE m.conversation.id = :conversationId
            AND m.isDeleted = false
            ORDER BY m.sentAt DESC
            """)
    Page<Message> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId
            AND m.sentAt > :lastSeenAt
            AND m.sender.id != :currentUserId
            AND m.isDeleted = false
            """)
    Long countUnreadMessages(@Param("conversationId") Long conversationId,
                             @Param("lastSeenAt") Instant lastSeenAt,
                             @Param("currentUserId") Long currentUserId);
}
