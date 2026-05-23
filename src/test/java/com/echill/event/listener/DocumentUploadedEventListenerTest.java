package com.echill.event.listener;

import com.echill.event.DocumentUploadedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class DocumentUploadedEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void testDocumentUploadedEventPropagation() throws InterruptedException {
        String testPdfUrl = "http://127.0.0.1:9090/pdf-test.pdf";
        String testTitle = "pdf-test";
        
        System.out.println("🚀 [TEST] Chạy transaction và publish DocumentUploadedEvent...");
        
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new DocumentUploadedEvent(testPdfUrl, testTitle));
            return null;
        });
        
        System.out.println("⏳ [TEST] Chờ 25 giây để luồng Asynchronous thực hiện tải và upload lên RAG Chatbot...");
        Thread.sleep(25000);
        System.out.println("✅ [TEST] Kết thúc thời gian chờ.");
    }
}
