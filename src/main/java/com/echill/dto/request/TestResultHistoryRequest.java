package com.echill.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TestResultHistoryRequest extends BasePageRequest {

    private Long testId;
    private String testTitle;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public TestResultHistoryRequest() {
        this.setSortBy("createdAt");
        this.setSortDir("desc");
    }

    @Override
    @JsonIgnore
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "totalScore", "timeTakenSeconds", "isPassed");
    }

}
