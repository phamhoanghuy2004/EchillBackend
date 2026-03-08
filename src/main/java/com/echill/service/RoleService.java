package com.echill.service;

import com.echill.dto.request.RoleCreationRequest;
import com.echill.dto.request.RoleUpdateRequest;
import com.echill.dto.response.RoleResponse;
import com.echill.entity.Permission;
import com.echill.entity.Role;
import com.echill.entity.User;
import com.echill.entity.UserRole;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.RoleMapper;
import com.echill.repository.PermissionRepository;
import com.echill.repository.RoleRepository;
import com.echill.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;
    PermissionRepository permissionRepository;
    UserRepository userRepository;

    @Transactional
    public RoleResponse create (RoleCreationRequest request){
        if (roleRepository.existsByName(request.getName())) {
            throw new AppException(ErrorEnum.ROLE_EXISTED);
        }

        var role = roleMapper.toRole(request);

        if (!CollectionUtils.isEmpty(request.getPermissions())) {
            List<Permission> permissions = permissionRepository.findAllByNameIn(request.getPermissions());
            if (permissions.size() != request.getPermissions().size()) {
                throw new AppException(ErrorEnum.PERMISSION_NOT_EXIST);
            }

            role.addPermissions(permissions);
        }

        roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @Transactional
    public RoleResponse update (String roleName, RoleUpdateRequest request){
        Role role = roleRepository.findWithPermissionsByName(roleName)
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

        role.setDescription(request.getDescription());

        if (request.getPermissions() != null) {
            List<Permission> newPermissions = permissionRepository.findAllByNameIn(request.getPermissions());

            if (newPermissions.size() != request.getPermissions().size()) {
                throw new AppException(ErrorEnum.PERMISSION_NOT_EXIST);
            }

            role.syncPermissions(newPermissions);
        }

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    @Transactional
    public void delete(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));


        if ("ADMIN".equals(role.getName()) || "STUDENT".equals(role.getName()) || "TEACHER".equals(role.getName())) {
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }

        roleRepository.delete(role);
        log.info("Đã xóa hoàn toàn Role: {}", roleName);
    }

    @Transactional(readOnly = true)
    public RoleResponse getByName(String roleName) {
        Role role = roleRepository.findWithPermissionsByName(roleName)
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

        return roleMapper.toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRolesByUserId(Long userId) {

        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(roleMapper::toRoleResponse)
                .toList();
    }
}
