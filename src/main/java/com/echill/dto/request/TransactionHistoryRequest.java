package com.echill.dto.request;

import com.echill.entity.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionHistoryRequest extends BasePageRequest {

    TransactionType type;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate;

    public TransactionHistoryRequest() {
        super.setSortBy("createdAt");
        super.setSortDir("desc");
    }

    @Override
    @JsonIgnore
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "totalAmount", "totalCoinsChanged");
    }

    @JsonIgnore
    public Instant getStartInstant() {
        if (this.startDate == null) return null;
        return this.startDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    }

    @JsonIgnore
    public Instant getEndInstant() {
        if (this.endDate == null) return null;
        return this.endDate.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    }

}