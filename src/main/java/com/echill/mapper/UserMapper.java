package com.echill.mapper;

import com.echill.dto.request.StudentRegisterRequest;
import com.echill.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toUser (StudentRegisterRequest studentRegisterRequest);
}
