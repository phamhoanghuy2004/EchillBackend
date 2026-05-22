package com.echill.repository.specification;

import com.echill.dto.request.AdminCourseSearchRequest;
import com.echill.entity.Course;
import com.echill.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CourseSpecification {
    public static Specification<Course> filter(AdminCourseSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getCourseName())) {
                String pattern = "%" + request.getCourseName().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (StringUtils.hasText(request.getTeacherName())) {
                String pattern = "%" + request.getTeacherName().toLowerCase() + "%";
                Join<Course, User> teacherJoin = root.join("teacher");
                predicates.add(cb.like(cb.lower(teacherJoin.get("fullName")), pattern));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
