package com.echill.dto.request;

import com.echill.entity.enums.Status;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCourseSearchRequest extends BasePageRequest {
    String courseName;
    String teacherName;
    Status status;

    public AdminCourseSearchRequest() {
        this.setSortBy("createdAt");
        this.setSortDir("desc");
    }

    @Override
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "name", "status");
    }
}
