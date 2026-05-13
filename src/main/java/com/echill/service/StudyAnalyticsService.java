package com.echill.service;

import com.echill.dto.response.MonthlyStudyActivityResponse;
import com.echill.dto.response.WeeklyStudyTimeResponse;
import com.echill.entity.DailyStudyTime;
import com.echill.entity.User;
import com.echill.repository.DailyStudyTimeRepository;
import com.echill.repository.TransactionRepository;
import com.echill.repository.UserRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StudyAnalyticsService {
    DailyStudyTimeRepository dailyStudyTimeRepository;
    UserRepository userRepository;
    TransactionRepository transactionRepository;
    /**
     * API 1: Dùng để Frontend Ping (Cứ 30s gọi 1 lần khi đang ở màn hình học)
     */
    @Transactional
    public void pingStudyTime(Long userId, Long addedSeconds) {
        if (addedSeconds <= 0 || addedSeconds > 300) {
            // Anti-cheat: Chặn hacker gọi postman cộng 1 tỷ giây. Tối đa 5 phút cho 1 ping.
            return;
        }

        // ĐỒNG BỘ TIMEZONE VỚI HÀM READ (Rất quan trọng)
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);

        // Thử Update Atomic. Nếu record đã tồn tại, DB tự cộng an toàn tuyệt đối.
        int updatedRows = dailyStudyTimeRepository.addSecondsAtomic(userId, today, addedSeconds);

        // Nếu updatedRows = 0, nghĩa là hôm nay là ngày mới, chưa có record nào -> Tạo mới
        if (updatedRows == 0) {
            try {
                User user = userRepository.getReferenceById(userId); // Ko cần fetch, chỉ cần proxy
                DailyStudyTime newRecord = DailyStudyTime.builder()
                        .user(user)
                        .studyDate(today)
                        .totalSeconds(addedSeconds)
                        .build();
                dailyStudyTimeRepository.saveAndFlush(newRecord);
            } catch (DataIntegrityViolationException e) {
                // Anti-race-condition: Nếu 2 luồng cùng lúc thấy updatedRows=0 và cùng INSERT
                // Luồng 2 sẽ bị DataIntegrity (do Unique Index). Khi đó ta chỉ cần gọi đè update lần nữa.
                log.warn("Race condition when inserting study time userId={}", userId, e);
                dailyStudyTimeRepository.addSecondsAtomic(userId, today, addedSeconds);
            }
        }
    }

    /**
     * API 2: Lấy dữ liệu vẽ Biểu đồ Tuần này
     */
    @Transactional(readOnly = true)
    public WeeklyStudyTimeResponse getTotalStudySecondsThisWeek() {
        Long userId = SecurityUtils.getCurrentUserId();

        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        LocalDate startOfWeekDate = now.with(DayOfWeek.MONDAY).toLocalDate();
        LocalDate endOfWeekDate = startOfWeekDate.plusDays(6);

        Instant startOfWeekInstant = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant startOfNextWeekInstant = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS).plusDays(7).toInstant();

        Long currentSeconds = dailyStudyTimeRepository.sumUserStudySecondsInDateRange(userId, startOfWeekDate, endOfWeekDate);

        boolean hasClaimed = transactionRepository.existsBonusTransactionInWeek(
                userId,
                startOfWeekInstant,
                startOfNextWeekInstant
        );

        Long targetRewardSeconds = 36000L;

        return WeeklyStudyTimeResponse.builder()
                .currentSeconds(currentSeconds != null ? currentSeconds : 0L)
                .targetSeconds(targetRewardSeconds)
                .isClaimed(hasClaimed)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyStudyActivityResponse getMonthlyStudyActivity(int year, int month) {
        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<DailyStudyTime> studyTimes = dailyStudyTimeRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        Map<Integer, Long> dailyDataMap = studyTimes.stream()
                .collect(Collectors.toMap(
                        record -> record.getStudyDate().getDayOfMonth(),
                        DailyStudyTime::getTotalSeconds
                ));

        return MonthlyStudyActivityResponse.builder()
                .dailyData(dailyDataMap)
                .build();
    }
}
