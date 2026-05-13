package com.echill.dto.request;

import com.echill.entity.enums.TestType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSetSearchRequest extends BasePageRequest {
    String keyword; // Tìm theo title
    Integer year;   // Tìm theo năm
    TestType type;  // Bắt buộc từ FE truyền xuống (TOEIC, IELTS...)

    public TestSetSearchRequest() {
        super.setSortBy("createdAt");
        super.setSortDir("desc");
    }

    @Override
    protected List<String> getAllowedSortColumns() {
        // Cho phép sort theo ngày tạo hoặc năm phát hành
        return List.of("createdAt", "year", "title");
    }
}
