package com.echill.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class LessonProgressBatchRepository {

    private final JdbcTemplate jdbcTemplate;
    public record ProgressBatchItem(Long lessonId, Long userId, Integer currentSecond) {}

    public void batchUpdateProgress(List<ProgressBatchItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = """
            UPDATE lesson_progresses lp
            INNER JOIN enrollments e ON lp.enrollment_id = e.id
            SET lp.last_watched_second = GREATEST(lp.last_watched_second, ?)
            WHERE e.student_id = ? AND lp.lesson_id = ?
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ProgressBatchItem item = items.get(i);

                ps.setInt(1, item.currentSecond());

                ps.setLong(2, item.userId());

                ps.setLong(3, item.lessonId());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        log.info("🚀 [Batch Update] Đã đóng gói và cập nhật thành công {} tiến độ xuống MySQL.", items.size());
    }
}