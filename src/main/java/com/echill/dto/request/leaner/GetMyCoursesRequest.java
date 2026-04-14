package com.echill.dto.request.leaner;

import com.echill.dto.request.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetMyCoursesRequest extends BasePageRequest {

    @Override
    protected List<String> getAllowedSortColumns() {
        // Cho phép sort theo thuộc tính của Entity Enrollment
        return List.of("createdAt", "lastAccessedAt", "courseName");
    }
}
