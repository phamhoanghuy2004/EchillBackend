package com.echill.service;

import com.echill.dto.request.AdminUserSearchRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.UserResponse;
import com.echill.entity.Role;
import com.echill.entity.User;
import com.echill.entity.UserRole;
import com.echill.entity.enums.Status;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.UserMapper;
import com.echill.repository.UserRepository;
import com.echill.repository.specification.UserSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.echill.dto.response.AdminUserCourseDto;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserService {

    UserRepository userRepository;
    UserMapper userMapper;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(AdminUserSearchRequest request) {
        Specification<User> spec = UserSpecification.filter(request);

        Page<User> pageData = userRepository.findAll(spec, request.getPageable());

        Page<UserResponse> dtoPage = pageData.map(userMapper::toUserResponse);

        return PageResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserDetail(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        
        UserResponse response = userMapper.toUserResponse(user);
        
        boolean isTeacher = user.getUserRoles().stream().anyMatch(ur -> "TEACHER".equals(ur.getRole().getName()));
        boolean isStudent = user.getUserRoles().stream().anyMatch(ur -> "STUDENT".equals(ur.getRole().getName()));
        
        List<AdminUserCourseDto> courses = new ArrayList<>();
        if (isTeacher) {
            List<Object[]> teacherCourses = courseRepository.findBasicInfoByTeacherId(id);
            courses = teacherCourses.stream()
                    .map(obj -> new AdminUserCourseDto((Long) obj[0], (String) obj[1]))
                    .collect(Collectors.toList());
        } else if (isStudent) {
            List<Object[]> studentCourses = enrollmentRepository.findCourseBasicInfoByStudentId(id);
            courses = studentCourses.stream()
                    .map(obj -> new AdminUserCourseDto((Long) obj[0], (String) obj[1]))
                    .collect(Collectors.toList());
        }
        
        response.setCourses(courses);
        
        return response;
    }

    @Transactional
    public UserResponse blockUser(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        if (user.getStatus() == Status.BLOCKED) {
            throw new AppException(ErrorEnum.USER_ALREADY_BLOCKED);
        }

        // Không cho phép block admin
        boolean isAdmin = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .anyMatch("ADMIN"::equals);

        if (isAdmin) {
            throw new AppException(ErrorEnum.CANNOT_BLOCK_ADMIN);
        }

        user.setStatus(Status.BLOCKED);
        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse unblockUser(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }
}
