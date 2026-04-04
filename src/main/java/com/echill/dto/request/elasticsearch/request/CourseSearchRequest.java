package com.echill.dto.request.elasticsearch.request;

import com.echill.entity.enums.CourseSortType;
import com.echill.entity.enums.Level;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseSearchRequest {

    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_PAGE = 0;

    private String keyword;
    private Long categoryId;
    private Level level;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Sort & Pagination
    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;
    private CourseSortType sortBy;

    public int getSafeSize() {
        if (this.size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(this.size, MAX_SIZE);
    }

    public int getSafePage() {
        if (this.page < 0) {
            return DEFAULT_PAGE;
        }
        return this.page;
    }
}