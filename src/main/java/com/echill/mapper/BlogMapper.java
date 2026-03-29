package com.echill.mapper;

import com.echill.dto.request.BlogRequest;
import com.echill.dto.response.BlogResponse;
import com.echill.entity.Blog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(source = "user.fullName", target = "authorName", defaultValue = "Unknown")
    BlogResponse toResponse(Blog blog);

    default LocalDateTime mapInstantToLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    void updateBlog(@MappingTarget Blog blog, BlogRequest blogRequest);
}