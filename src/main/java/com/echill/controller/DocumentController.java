package com.echill.controller;


import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.DocumentResponse;
import com.echill.service.DocumentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentController {

    DocumentService documentService;

    @PostMapping(value = "/{lessonId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable Long lessonId,
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {

        return ApiResponse.<DocumentResponse>builder()
                .message("Tải lên tài liệu thành công!")
                .data(documentService.uploadDocument(lessonId, title, file))
                .build();
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ApiResponse.<Void>builder()
                .message("Đã xóa tài liệu thành công!")
                .build();
    }

    @GetMapping("/{lessonId}/documents")
    // Tùy logic nghiệp vụ: Có thể mở cho Học sinh đã mua khóa học, hoặc chỉ cho Teacher.
    // Ở đây tôi giả sử là ai cũng xem được danh sách tài liệu (nhưng tải được hay không là chuyện khác)
    public ApiResponse<List<DocumentResponse>> getDocumentsByLesson(@PathVariable Long lessonId) {

        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Lấy danh sách tài liệu thành công!")
                .data(documentService.getDocumentsByLessonId(lessonId))
                .build();
    }
}
