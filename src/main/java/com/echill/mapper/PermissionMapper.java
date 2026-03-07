package com.echill.mapper;

import com.echill.dto.request.PermissionCreationRequest;
import com.echill.dto.response.PermissionResponse;
import com.echill.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission (PermissionCreationRequest request);
    PermissionResponse toPermissionResponse (Permission permission);
}
