package com.echill.service;

import com.echill.dto.response.TestSectionSummaryDto;
import com.echill.repository.TestSectionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestSectionService {
    TestSectionRepository testSectionRepository;
    @Lazy
    @Autowired
    @lombok.experimental.NonFinal
    TestSectionService self;


    public List<TestSectionSummaryDto> getTestSectionSummaries(Long testId) {
        return self.getCachedSectionSummaries(testId);
    }


    @Cacheable(cacheNames = "testSectionSummaries", key = "#testId", sync = true)
    @Transactional(readOnly = true)
    public List<TestSectionSummaryDto> getCachedSectionSummaries(Long testId) {
        return testSectionRepository.findSectionSummariesByTestId(testId);
    }
}
