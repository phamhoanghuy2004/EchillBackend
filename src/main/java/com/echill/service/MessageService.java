package com.echill.service;

import com.echill.dto.response.MessageResponse;
import com.echill.entity.Conversation;
import com.echill.entity.Message;
import com.echill.entity.Participant;
import com.echill.entity.User;
import com.echill.entity.enums.MessageType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    ConversationRepository conversationRepository;
    UserRepository userRepository;
    ParticipantRepository participantRepository;
    SimpMessagingTemplate messagingTemplate;

    /**
     * Lấy tin nhắn của conversation theo pagination (mới nhất trước).
     *
     * @param conversationId ID conversation
     * @param page           Trang (bắt đầu từ 0)
     * @param size           Số tin nhắn mỗi trang
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, int page, int size) {
        // Kiểm tra conversation tồn tại
        if (!conversationRepository.existsById(conversationId)) {
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }

        return messageRepository
                .findByConversationId(conversationId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt")))
                .map(this::toResponse);
    }

    /**
     * Lưu tin nhắn mới vào DB và cập nhật lastMessageAt của conversation.
     * Được gọi từ WebSocket handler.
     *
     * @param conversationId ID conversation
     * @param senderId       ID người gửi (từ JWT Principal)
     * @param content        Nội dung tin nhắn
     * @param messageType    Loại tin nhắn (TEXT / IMAGE)
     * @return MessageResponse để broadcast về client
     */
    @Transactional
    public MessageResponse saveAndBroadcast(Long conversationId, Long senderId,
                                            String content, MessageType messageType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorEnum.UNCATEGORIZED));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // Lưu tin nhắn
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .sentAt(Instant.now())
                .messageType(messageType != null ? messageType : MessageType.TEXT)
                .isDeleted(false)
                .build();

        message = messageRepository.save(message);

        // Cập nhật lastMessageAt của conversation
        conversation.setLastMessageAt(message.getSentAt());
        conversationRepository.save(conversation);

        MessageResponse response = toResponse(message);

        // Notify all participants via private queue for sidebar and active message updates
        participantRepository.findByConversationId(conversationId).forEach(p -> {
            messagingTemplate.convertAndSendToUser(
                p.getUser().getId().toString(),
                "/queue/messages",
                response
            );
        });

        log.debug("Message [{}] saved in conversation [{}] by sender [{}]",
                message.getId(), conversationId, senderId);

        return response;
    }

    // ====================== MAPPER ==========================

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatarUrl())
                .content(message.getIsDeleted() ? "Tin nhắn đã bị xóa" : message.getContent())
                .messageType(message.getMessageType())
                .sentAt(message.getSentAt())
                .isDeleted(message.getIsDeleted())
                .build();
    }
}
