package com.echill.event.listener;

import com.echill.event.DocumentUploadedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentUploadedEventListener {

    @Async("ioTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        log.info("🎯 Nhận được tín hiệu Database đã Commit xong cho tài liệu: {}. Bắt đầu đồng bộ lên RAG Chatbot...", event.title());
        
        RestTemplate restTemplate = new RestTemplate();
        
        // 1. Download file from Cloudinary URL
        byte[] fileBytes;
        try {
            org.springframework.http.HttpHeaders getHeaders = new org.springframework.http.HttpHeaders();
            getHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            org.springframework.http.HttpEntity<Void> getEntity = new org.springframework.http.HttpEntity<>(getHeaders);
            org.springframework.http.ResponseEntity<byte[]> getResponse = restTemplate.exchange(
                    event.fileUrl(),
                    org.springframework.http.HttpMethod.GET,
                    getEntity,
                    byte[].class
            );
            fileBytes = getResponse.getBody();
        } catch (Exception e) {
            log.error("Lỗi khi tải file từ Cloudinary: {}", event.fileUrl(), e);
            return;
        }
        
        if (fileBytes == null || fileBytes.length == 0) {
            log.error("Nội dung file trống từ Cloudinary: {}", event.fileUrl());
            return;
        }

        // 2. Prepare filename (ensure it has .pdf extension)
        String cleanTitleTemp = event.title().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!cleanTitleTemp.toLowerCase().endsWith(".pdf")) {
            cleanTitleTemp += ".pdf";
        }
        final String cleanTitle = cleanTitleTemp;

        // 3. Generate JWT Token
        String token = generateRAGChatbotToken();

        // 4. Call Chatbot Upload API
        String uploadUrl = "http://127.0.0.1:8000/api/upload/";

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        
        org.springframework.http.HttpHeaders partHeaders = new org.springframework.http.HttpHeaders();
        partHeaders.setContentDispositionFormData("pdf", cleanTitle);
        partHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        org.springframework.http.HttpEntity<byte[]> fileEntity = new org.springframework.http.HttpEntity<>(fileBytes, partHeaders);
        
        body.add("pdf", fileEntity);

        org.springframework.http.converter.FormHttpMessageConverter converter = new org.springframework.http.converter.FormHttpMessageConverter();
        SimpleHttpOutputMessage outputMessage = new SimpleHttpOutputMessage();
        try {
            converter.write(body, org.springframework.http.MediaType.MULTIPART_FORM_DATA, outputMessage);
        } catch (java.io.IOException e) {
            log.error("Lỗi khi serialize body cho RAG Chatbot: {}", e.getMessage(), e);
            return;
        }

        byte[] requestBodyBytes = outputMessage.getBytes();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(outputMessage.getHeaders().getContentType());
        headers.setContentLength(requestBodyBytes.length);
        headers.add("Cookie", "access_token=" + token);

        org.springframework.http.HttpEntity<byte[]> requestEntity = 
                new org.springframework.http.HttpEntity<>(requestBodyBytes, headers);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Đã đồng bộ tài liệu '{}' lên RAG Chatbot thành công!", cleanTitle);
            } else {
                log.error("Đồng bộ tài liệu lên RAG Chatbot thất bại. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Lỗi kết nối khi đồng bộ tài liệu lên RAG Chatbot: {}", e.getMessage(), e);
        }
    }

    private String generateRAGChatbotToken() {
        try {
            String signerKey = "Pb6zWmUnYq0Wk6O00k5yiQEb+U6PIY+6B1zmH84HMsLPApdlwj0sc3jecsI/Bu88";
            byte[] sharedKey = signerKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            com.nimbusds.jose.JWSSigner signer = new com.nimbusds.jose.crypto.MACSigner(sharedKey);
            
            com.nimbusds.jose.JWSHeader header = new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256);
            
            com.nimbusds.jwt.JWTClaimsSet claimsSet = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                    .claim("user_id", 1)
                    .claim("roles", java.util.List.of("admin"))
                    .claim("user_name", "Hoang Huy")
                    .claim("type", "access")
                    .issueTime(new java.util.Date())
                    .expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(900))) // 15 mins
                    .build();
            
            com.nimbusds.jose.Payload payload = new com.nimbusds.jose.Payload(claimsSet.toJSONObject());
            com.nimbusds.jose.JWSObject jwsObject = new com.nimbusds.jose.JWSObject(header, payload);
            
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo JWT cho RAG Chatbot: " + e.getMessage(), e);
        }
    }

    private static class SimpleHttpOutputMessage implements org.springframework.http.HttpOutputMessage {
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        private final org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        
        @Override
        public java.io.OutputStream getBody() {
            return out;
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }
    }
}
