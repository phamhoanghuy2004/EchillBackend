package com.echill.repository.specification;

import com.echill.dto.request.AdminUserSearchRequest;
import com.echill.entity.Role;
import com.echill.entity.User;
import com.echill.entity.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    public static Specification<User> filter(AdminUserSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern)
                ));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (StringUtils.hasText(request.getRole())) {
                Join<User, UserRole> userRolesJoin = root.join("userRoles");
                Join<UserRole, Role> roleJoin = userRolesJoin.join("role");
                predicates.add(cb.equal(roleJoin.get("name"), request.getRole()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
