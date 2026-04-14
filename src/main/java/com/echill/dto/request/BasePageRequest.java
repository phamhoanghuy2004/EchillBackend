package com.echill.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
public class BasePageRequest {

    @Min(value = 1, message = "Trang phải bắt đầu từ 1")
    private int page = 1;

    @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    private int size = 10;

    private String sortBy = "lastAccessedAt";
    private String sortDir = "desc";

    // 💥 BÍ KÍP BẢO VỆ: Danh sách các cột ĐƯỢC PHÉP sắp xếp mặc định
    @JsonIgnore
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "updatedAt", "lastAccessedAt");
    }

    @JsonIgnore
    public Pageable getPageable() {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        String safeSortBy = getAllowedSortColumns().contains(sortBy) ? sortBy : "lastAccessedAt";

        return PageRequest.of(this.page - 1, this.size, Sort.by(direction, safeSortBy));
    }
}
