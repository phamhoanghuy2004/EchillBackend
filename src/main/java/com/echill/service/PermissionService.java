package com.echill.service;

import com.echill.dto.request.PermissionCreationRequest;
import com.echill.dto.response.PermissionResponse;
import com.echill.entity.Permission;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.PermissionMapper;
import com.echill.repository.PermissionRepository;
import com.echill.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PermissionService {
    PermissionRepository permissionRepository;
    RoleRepository roleRepository;
    PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionCreationRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new AppException(ErrorEnum.PERMISSION_EXISTED);
        }

        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);

    }

    public List<PermissionResponse> getAll() {
       return permissionRepository.findAll().stream()
               .map(permissionMapper::toPermissionResponse)
               .toList();
    }

    public List<PermissionResponse> getAllByRoleId(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new AppException(ErrorEnum.ROLE_NOT_EXIST);
        }

        return permissionRepository.findAllByRoleId(roleId).stream()
                .map(permissionMapper::toPermissionResponse)
                .toList();
    }

    public void delete(Long id){
        permissionRepository.deletePermissionById(id);
        log.info("Đã xóa Permission có ID: {}", id);
    }
 }
