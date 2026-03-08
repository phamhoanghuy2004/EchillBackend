package com.echill.mapper;

import com.echill.dto.request.RoleCreationRequest;
import com.echill.dto.response.PermissionResponse;
import com.echill.dto.response.RoleResponse;
import com.echill.entity.Role;
import com.echill.entity.RolePermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "rolePermissions", ignore = true)
    Role toRole (RoleCreationRequest request);

    @Mapping(source = "rolePermissions", target = "permissions")
    RoleResponse toRoleResponse (Role role);

    @Mapping(target = "name", source = "permission.name")
    @Mapping(target = "description", source = "permission.description")
    PermissionResponse toPermissionResponse (RolePermission rolePermission);
}
