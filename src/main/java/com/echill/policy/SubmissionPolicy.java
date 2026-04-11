package com.echill.policy;

import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SubmissionPolicy {
    private static final int GRACE_PERIOD_SECONDS = 60;
    private static final int MAX_PAYLOAD_SIZE = 200;

    public boolean isLate(LocalDateTime endTime, LocalDateTime submitTime) {
        return submitTime.isAfter(endTime.plusSeconds(GRACE_PERIOD_SECONDS));
    }

    public void validatePayload(Map<Long, Long> answers) {
        if (answers != null && answers.size() > MAX_PAYLOAD_SIZE) {
            throw new AppException(StudentErrorEnum.PAYLOAD_TOO_LARGE);
        }
    }
}
