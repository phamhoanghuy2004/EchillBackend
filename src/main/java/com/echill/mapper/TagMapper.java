package com.echill.mapper;

import com.echill.dto.response.TagResponse;
import com.echill.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {
    @Mapping(source = "tagGroup.displayName", target = "groupDisplayName")
    TagResponse toResponse(Tag tag);
}
