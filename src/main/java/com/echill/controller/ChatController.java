package com.echill.controller;

import com.echill.dto.request.ConversationRequest;
import com.echill.dto.request.MessageRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.ConversationResponse;
import com.echill.dto.response.MessageResponse;
import com.echill.dto.response.PageResponse;
import com.echill.service.ConversationService;
import com.echill.service.MessageService;
import com.echill.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    ConversationService conversationService;
    MessageService messageService;
    SimpMessagingTemplate messagingTemplate;
    /**
     * GET /conversations/{userId}
     * Lấy danh sách conversations của user (dùng cho Student xem list teachers,
     * hoặc Teacher xem danh sách chats đang mở).
     */
    @GetMapping("/conversations/{userId}")
    public ApiResponse<List<ConversationResponse>> getConversations(@PathVariable Long userId) {
        return ApiResponse.<List<ConversationResponse>>builder()
                .data(conversationService.getConversationsByUserId(userId))
                .build();
    }

    /**
     * GET /messages/{conversationId}?page=0&size=20
     * Lấy tin nhắn theo trang (mới nhất trước).
     * Frontend load trang 0 đầu tiên, muốn load thêm thì tăng page.
     */
    @GetMapping("/messages/{conversationId}")
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.<PageResponse<MessageResponse>>builder()
                .data(PageResponse.of(messageService.getMessages(conversationId, page, size)))
                .build();
    }

    /**
     * POST /conversations/{conversationId}/seen
     * Đánh dấu đã xem toàn bộ tin nhắn trong cuộc hội thoại.
     */
    @PostMapping("/conversations/{conversationId}/seen")
    public ApiResponse<Void> markSeenAsRead(@PathVariable Long conversationId) {
        conversationService.updateLastSeen(conversationId, SecurityUtils.getCurrentUserId());
        return ApiResponse.<Void>builder().build();
    }

    /**
     * POST /conversations/create-or-get
     * Teacher click icon chat → gọi API này để lấy hoặc tạo conversation 1-1.
     */
    @PostMapping("/conversations/create-or-get")
    public ApiResponse<ConversationResponse> createOrGetConversation(
            @Valid @RequestBody ConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .data(conversationService.getOrCreateConversation(
                        request.getTeacherId(), request.getStudentId()))
                .build();
    }

    // ================================================================
    // WEBSOCKET HANDLER
    // ================================================================

    /**
     * Client gửi tin nhắn → /app/chat.send
     * Server: lưu DB → update lastMessageAt → broadcast /topic/conversation/{id}
     *
     * @param request       Payload từ client (conversationId, content, messageType)
     * @param authentication JWT principal được inject tự động bởi Spring Security
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid MessageRequest request, Authentication authentication) {
        // Lấy senderId từ JWT claim (stateless, không dùng SecurityContext trực tiếp)
        Long senderId = extractUserId(authentication);

        // Lưu vào DB và lấy response
        MessageResponse response = messageService.saveAndBroadcast(
                request.getConversationId(),
                senderId,
                request.getContent(),
                request.getMessageType()
        );

        log.debug("Message saved and notified via user queue for conversation {}", request.getConversationId());
    }

    // ================================================================
    // PRIVATE HELPER
    // ================================================================

    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Number userIdNumber = jwt.getClaim("userId");
            if (userIdNumber != null) {
                return userIdNumber.longValue();
            }
        }
        throw new IllegalStateException("Cannot extract userId from WebSocket authentication");
    }
}
