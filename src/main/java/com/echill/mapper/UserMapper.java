package com.echill.mapper;

import com.echill.dto.response.RoleResponse;
import com.echill.dto.response.UserResponse;
import com.echill.entity.User;
import com.echill.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user.getUserRoles()))")
    UserResponse toUserResponse(User user);

    default Set<RoleResponse> mapRoles(Set<UserRole> userRoles) {
        if (userRoles == null) {
            return null;
        }
        return userRoles.stream().map(ur -> 
            RoleResponse.builder()
                .name(ur.getRole().getName())
                .description(ur.getRole().getDescription())
                .build()
        ).collect(Collectors.toSet());
    }
}
