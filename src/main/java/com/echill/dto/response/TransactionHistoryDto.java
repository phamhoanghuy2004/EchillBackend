package com.echill.dto.response;

import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryDto(
        Long id,
        Instant createdAt,
        String description,
        Long totalCoinsChanged,
        BigDecimal totalAmount,
        Long balanceAfter,
        TransactionStatus status,
        TransactionType type
) {}