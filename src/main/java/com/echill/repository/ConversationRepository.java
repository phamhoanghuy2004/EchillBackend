package com.echill.repository;

import com.echill.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Tìm conversation 1-1 giữa 2 user (không phải group).
     * Dùng GROUP BY + HAVING để đảm bảo cả 2 người đều là participants.
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.isGroup = false
            AND (SELECT COUNT(p) FROM Participant p WHERE p.conversation = c AND p.user.id IN (:id1, :id2)) = 2
            """)
    Optional<Conversation> findDirectConversation(@Param("id1") Long id1, @Param("id2") Long id2);
}
