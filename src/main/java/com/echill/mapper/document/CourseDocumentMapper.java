package com.echill.mapper.document;

import com.echill.document.CourseDocument;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface CourseDocumentMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.description", target = "categoryDescription")
    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "teacher.avatarUrl", target = "teacherAvatarUrl")
    @Mapping(target = "createdAt", expression = "java(course.getCreatedAt() != null ? course.getCreatedAt().toEpochMilli() : null)")
    @Mapping(target = "discountPercent", expression = "java(course.getDiscountPercent())")
    CourseDocument toDocument(Course course);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toInstant")
    CourseCardResponse toResponse(CourseDocument document);

    @Named("toInstant")
    default Instant toInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }
}
