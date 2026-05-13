package com.echill.repository.projection;

import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionHistoryProjection {
    Long getId();
    Instant getCreatedAt();
    String getDescription();
    Long getTotalCoinsChanged();
    BigDecimal getTotalAmount();
    Long getBalanceAfter();
    TransactionStatus getStatus();
    TransactionType getType();
}
