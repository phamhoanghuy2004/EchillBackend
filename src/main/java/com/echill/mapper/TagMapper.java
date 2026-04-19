package com.echill.mapper;

import com.echill.dto.response.TagResponse;
import com.echill.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {
    TagResponse toResponse(Tag tag);
}
