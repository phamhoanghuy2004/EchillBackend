package com.echill.service;

import com.echill.dto.request.TransactionHistoryRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TransactionHistoryDto;
import com.echill.entity.Transaction;
import com.echill.repository.TransactionRepository;
import com.echill.repository.projection.TransactionHistoryProjection;
import com.echill.repository.specification.TransactionSpecification;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionService {
    TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryDto> getMyTransactions(TransactionHistoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Gọi Specification
        Specification<Transaction> spec = TransactionSpecification.filterHistory(
                userId, request.getType(), request.getStartInstant(), request.getEndInstant()
        );

        // Fetch data bọc qua Interface
        Page<TransactionHistoryProjection> projectionPage = transactionRepository.findBy(spec, query ->
                query.as(TransactionHistoryProjection.class).page(request.getPageable())
        );

        // Map sang Record DTO trả về cho Controller
        Page<TransactionHistoryDto> dtoPage = projectionPage.map(p -> new TransactionHistoryDto(
                p.getId(), p.getCreatedAt(), p.getDescription(), p.getTotalCoinsChanged(),
                p.getTotalAmount(), p.getBalanceAfter(), p.getStatus(), p.getType()
        ));

        return PageResponse.of(dtoPage);
    }
}
