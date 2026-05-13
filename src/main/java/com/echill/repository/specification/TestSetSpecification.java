package com.echill.repository.specification;

import com.echill.entity.TestSet;
import com.echill.entity.enums.TestType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TestSetSpecification {

    public static Specification<TestSet> buildSearch(String keyword, Integer year, TestType type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            } else {

                List<TestType> allowedTypes = new ArrayList<>();
                for (String typeString : TestType.getPracticePageTypes()) {
                    allowedTypes.add(TestType.valueOf(typeString));
                }

                predicates.add(root.get("type").in(allowedTypes));
            }

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
            }

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}