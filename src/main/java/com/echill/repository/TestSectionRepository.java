package com.echill.repository;

import com.echill.dto.response.TestSectionSummaryDto;
import com.echill.entity.TestSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestSectionRepository extends JpaRepository<TestSection,Long> {

    @Query("SELECT new com.echill.dto.response.TestSectionSummaryDto(" +
            "s.id, s.title, s.orderIndex, COUNT(DISTINCT q.id)) " +
            "FROM TestSection s " +
            "LEFT JOIN s.questions q " +
            "WHERE s.test.id = :testId " +
            "GROUP BY s.id, s.title, s.orderIndex " +
            "ORDER BY s.orderIndex ASC")
    List<TestSectionSummaryDto> findSectionSummariesByTestId(@Param("testId") Long testId);
}
