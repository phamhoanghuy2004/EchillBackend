package com.echill.dto.request;

import com.echill.entity.enums.ConsultationStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultationSearchRequest extends BasePageRequest {
    String keyword;
    ConsultationStatus status;
    Long adminId;

    public ConsultationSearchRequest() {
        this.setSortBy("createdAt");
        this.setSortDir("desc");
    }

    @Override
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "fullName", "status");
    }
}
