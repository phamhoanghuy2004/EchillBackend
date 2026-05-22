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
public class AdminUserSearchRequest extends BasePageRequest {
    String keyword;
    String role;
    Status status;

    public AdminUserSearchRequest() {
        this.setSortBy("createdAt");
        this.setSortDir("desc");
    }

    @Override
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "fullName", "email", "status");
    }
}
