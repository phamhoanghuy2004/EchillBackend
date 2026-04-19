package com.echill.repository;

import com.echill.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByConversationIdAndUserId(Long conversationId, Long userId);

    List<Participant> findByConversationId(Long conversationId);

    /**
     * Lấy tất cả conversations của 1 user kèm theo conversation và user khác trong conversation.
     * Dùng cho Student xem danh sách giáo viên đã chat.
     */
    @Query("""
            SELECT p FROM Participant p
            JOIN FETCH p.conversation c
            JOIN FETCH p.user u
            WHERE p.user.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    List<Participant> findByUserIdWithConversation(@Param("userId") Long userId);

    /**
     * Lấy participant còn lại trong conversation (người kia).
     */
    @Query("""
            SELECT p FROM Participant p
            JOIN FETCH p.user u
            WHERE p.conversation.id = :conversationId
            AND p.user.id != :excludeUserId
            """)
    List<Participant> findOtherParticipants(@Param("conversationId") Long conversationId,
                                            @Param("excludeUserId") Long excludeUserId);
}
