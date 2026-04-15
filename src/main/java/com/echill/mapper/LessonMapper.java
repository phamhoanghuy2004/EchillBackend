package com.echill.mapper;

import com.echill.dto.response.LessonResponse;
import com.echill.dto.response.guest.LessonPublicResponse;
import com.echill.dto.response.learner.LessonDetailResponse;
import com.echill.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DocumentMapper.class})
public interface LessonMapper {
    @Mapping(source = "course.id", target = "courseId")
    LessonResponse toLessonResponse(Lesson lesson);

    // Hàm này là để map cho Public DTO
    // Và thằng này sẽ tự động gọi cái hàm chuyển DocumentPublic và lấy data
    @Mapping(source = "hlsUrl", target = "previewVideoUrl")
    @Mapping(source = "testSet.id", target = "testSetId")
    @Mapping(target = "hasTest", expression = "java(lesson.getTestSet() != null)")
    LessonPublicResponse toLessonPublicResponse(Lesson lesson);

    LessonDetailResponse toLessonDetailResponse(Lesson lesson);
}
