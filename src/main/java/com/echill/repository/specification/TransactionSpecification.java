package com.echill.repository.specification;

import com.echill.entity.Transaction;
import com.echill.entity.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> filterHistory(
            Long userId, TransactionType type, Instant startDate, Instant endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Luôn luôn lọc theo user hiện tại
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Chỉ sinh ra WHERE type = ? khi user có chọn filter
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 3. Lọc theo thời gian
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}