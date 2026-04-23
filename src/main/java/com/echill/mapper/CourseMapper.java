package com.echill.mapper;

import com.echill.dto.response.guest.CourseDetailResponse;
import com.echill.dto.response.guest.LessonPublicResponse;
import com.echill.dto.response.guest.DocumentPublicResponse;
import com.echill.entity.Course;
import com.echill.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", uses = {LessonMapper.class})
public interface CourseMapper {

    // Hàm này để map cho Public DTO
    // Và nó cũng sẽ tự gọi  sang hàm map LessonPublic
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "teacher.avatarUrl", target = "teacherAvatarUrl")
    CourseDetailResponse toDetailResponse(Course course);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.description", target = "categoryDescription")
    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "teacher.avatarUrl", target = "teacherAvatarUrl")
    com.echill.dto.request.elasticsearch.response.CourseCardResponse toCardResponse(Course course);

    List<com.echill.dto.request.elasticsearch.response.CourseCardResponse> toCardResponseList(List<Course> courses);

}
