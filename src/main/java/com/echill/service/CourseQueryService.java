package com.echill.service;

import com.echill.constant.CacheNames;
import com.echill.dto.response.guest.CourseDetailResponse;
import com.echill.entity.Course;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.CourseMapper;
import com.echill.repository.CourseRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseQueryService {
    CourseRepository courseRepository;
    CourseMapper courseMapper;

    @Cacheable(cacheNames = CacheNames.COURSE_DETAIL, key = "'course:' + #id", sync = true)
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long id) {
        log.info("⚡ CHẠY VÀO DB ĐỂ LẤY CHI TIẾT KHÓA HỌC ID: {} (CACHE MISS)", id);

        Course course = courseRepository.findActiveCourseWithFullDetails(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        CourseDetailResponse courseDetailResponse = courseMapper.toDetailResponse(course);

        hideSensitiveData(courseDetailResponse);

        return courseDetailResponse;
    }

    private void hideSensitiveData(CourseDetailResponse response) {
        if (response.getLessons() != null){
            response.getLessons().forEach(lesson -> {
                if (!Boolean.TRUE.equals(lesson.getIsPreview())){
                    lesson.setPreviewVideoUrl(null);
                    if (lesson.getDocuments() != null){
                        lesson.getDocuments().forEach(document -> {
                            document.setFileUrl(null);
                        });
                    }
                }
            });
        }
    }
}
