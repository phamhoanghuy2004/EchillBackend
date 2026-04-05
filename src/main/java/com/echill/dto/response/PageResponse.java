package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    int currentPage;     // Trang hiện tại (Thường FE tính từ 1, BE tính từ 0 nên cần cẩn thận)
    int totalPages;      // Tổng số trang
    int pageSize;        // Số record trên 1 trang
    long totalElements;  // Tổng số toàn bộ record trong DB
    List<T> content;     // Danh sách dữ liệu thực tế

    // 💥 Hàm tiện ích để tự động map từ Spring Page sang PageResponse siêu nhanh
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .currentPage(page.getNumber() + 1) // +1 để trả về cho Frontend tính từ trang 1 cho dễ làm UI
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .content(page.getContent())
                .build();
    }
}