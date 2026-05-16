package com.echill.repository.specification;

import com.echill.dto.request.ConsultationSearchRequest;
import com.echill.entity.Consultation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ConsultationSpecification {
    public static Specification<Consultation> filter(ConsultationSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(root.get("phoneNumber"), pattern)
                ));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getAdminId() != null) {
                predicates.add(cb.equal(root.get("handledBy").get("id"), request.getAdminId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}