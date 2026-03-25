package com.echill.mapper;

import com.echill.dto.response.LessonResponse;
import com.echill.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DocumentMapper.class})
public interface LessonMapper {
    @Mapping(source = "course.id", target = "courseId")
    LessonResponse toLessonResponse(Lesson lesson);
}
