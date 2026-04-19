package com.echill.service;

import com.echill.dto.response.ConversationResponse;
import com.echill.dto.response.ConversationSeenResponse;
import com.echill.dto.response.MessageResponse;
import com.echill.dto.response.ParticipantResponse;
import com.echill.entity.Conversation;
import com.echill.entity.Message;
import com.echill.entity.Participant;
import com.echill.entity.User;
import com.echill.entity.enums.ParticipantRole;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.ConversationRepository;
import com.echill.repository.MessageRepository;
import com.echill.repository.ParticipantRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {

    ConversationRepository conversationRepository;
    ParticipantRepository participantRepository;
    MessageRepository messageRepository;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;

    /**
     * Tạo mới hoặc lấy conversation 1-1 giữa teacher và student.
     * Nếu đã tồn tại → trả về conversation cũ.
     * Nếu chưa tồn tại → tạo mới + tạo 2 Participant.
     */
    @Transactional
    public ConversationResponse getOrCreateConversation(Long teacherId, Long studentId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Optional<Conversation> existing = conversationRepository.findDirectConversation(teacherId, studentId);
        if (existing.isPresent()) {
            return buildConversationResponse(existing.get(), teacherId);
        }

        // 3. Tạo Conversation mới
        Conversation conversation = Conversation.builder()
                .isGroup(false)
                .build();
        conversation = conversationRepository.save(conversation);

        // 4. Tạo Participant cho Teacher
        Participant teacherParticipant = Participant.builder()
                .conversation(conversation)
                .user(teacher)
                .role(ParticipantRole.TEACHER)
                .build();

        // 5. Tạo Participant cho Student
        Participant studentParticipant = Participant.builder()
                .conversation(conversation)
                .user(student)
                .role(ParticipantRole.STUDENT)
                .build();

        participantRepository.saveAll(List.of(teacherParticipant, studentParticipant));

        log.info("Created new conversation [{}] between teacher [{}] and student [{}]",
                conversation.getId(), teacherId, studentId);

        return buildConversationResponse(conversation, teacherId);
    }

    /**
     * Lấy danh sách tất cả conversations của 1 user (cả teacher lẫn student).
     * Kèm theo thông tin người kia trong conversation.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsByUserId(Long userId) {
        List<Participant> myParticipations = participantRepository.findByUserIdWithConversation(userId);

        List<ConversationResponse> result = new ArrayList<>();
        for (Participant myParticipation : myParticipations) {
            Conversation conv = myParticipation.getConversation();
            ConversationResponse response = buildConversationResponse(conv, userId);
            result.add(response);
        }
        return result;
    }

    /**
     * Cập nhật lastSeenAt khi user mở conversation.
     */
    @Transactional
    public void updateLastSeen(Long conversationId, Long userId) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresent(p -> {
                    Instant now = Instant.now();
                    p.setLastSeenAt(now);
                    participantRepository.save(p);

                    // Broadcast "Seen" event to the conversation topic
                    ConversationSeenResponse seenResponse = ConversationSeenResponse.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .lastSeenAt(now)
                            .build();

                    // Notify participants via private queue for seen status updates
                    participantRepository.findByConversationId(conversationId).forEach(participant -> {
                        messagingTemplate.convertAndSendToUser(
                            participant.getUser().getId().toString(),
                            "/queue/seen",
                            seenResponse
                        );
                    });
                });
    }

    // ====================== PRIVATE HELPERS ==========================

    private ConversationResponse buildConversationResponse(Conversation conversation, Long currentUserId) {
        List<Participant> otherParticipants = participantRepository
                .findOtherParticipants(conversation.getId(), currentUserId);

        List<ParticipantResponse> participantResponses = otherParticipants.stream()
                .map(p -> ParticipantResponse.builder()
                        .userId(p.getUser().getId())
                        .fullName(p.getUser().getFullName())
                        .avatarUrl(p.getUser().getAvatarUrl())
                        .role(p.getRole())
                        .lastSeenAt(p.getLastSeenAt())
                        .build())
                .toList();

        // Lấy tin nhắn cuối cùng để preview
        String lastMessageContent = null;
        var lastMsgPage = messageRepository.findByConversationId(
                conversation.getId(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sentAt"))
        );
        if (!lastMsgPage.isEmpty()) {
            Message lastMsg = lastMsgPage.getContent().get(0);
            lastMessageContent = lastMsg.getIsDeleted() ? "Tin nhắn đã bị xóa" : lastMsg.getContent();
        }

        // Tính unread (số tin nhắn sau lastSeenAt của current user)
        Participant myParticipant = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), currentUserId)
                .orElse(null);

        Long unreadCount = 0L;
        if (myParticipant != null) {
            Instant lastSeen = myParticipant.getLastSeenAt() != null 
                    ? myParticipant.getLastSeenAt() 
                    : Instant.EPOCH; // Nếu chưa xem bao giờ thì tính từ đầu
            unreadCount = messageRepository.countUnreadMessages(conversation.getId(), lastSeen, currentUserId);
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .isGroup(conversation.getIsGroup())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessageContent(lastMessageContent)
                .unreadCount(unreadCount)
                .participants(participantResponses)
                .build();
    }
}
