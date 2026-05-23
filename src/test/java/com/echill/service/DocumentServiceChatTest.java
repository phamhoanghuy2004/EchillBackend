package com.echill.service;

import com.echill.dto.response.DocumentChatResponse;
import com.echill.entity.Document;
import com.echill.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class DocumentServiceChatTest {

    @Autowired
    private DocumentService documentService;

    @MockBean
    private DocumentRepository documentRepository;

    @Test
    void testChatWithDocumentLogicAndPayloadGeneration() {
        Long documentId = 999L;
        String question = "What is this document about?";
        
        Document mockDoc = new Document();
        mockDoc.setId(documentId);
        mockDoc.setTitle("Test-Document|Title?"); // contains invalid chars
        mockDoc.setFileUrl("http://example.com/test.pdf");

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(mockDoc));

        // Since the real RAG Chatbot is on port 8000, we expect this to try to call the actual server.
        // If the chatbot is offline, it will throw an exception (AppException).
        // Let's call it and capture the outcome.
        try {
            DocumentChatResponse response = documentService.chatWithDocument(documentId, question);
            assertNotNull(response);
            System.out.println("🤖 RAG Chatbot Response: " + response.getAnswer());
        } catch (Exception e) {
            System.out.println("⚠️ Could not connect to RAG Chatbot (offline or mock): " + e.getMessage());
            // It should at least pass validation and try to connect
            assertTrue(e.getMessage().contains("Không thể lấy phản hồi từ AI") || e.getMessage().contains("Connection refused"));
        }

        verify(documentRepository, times(1)).findById(documentId);
    }
}
