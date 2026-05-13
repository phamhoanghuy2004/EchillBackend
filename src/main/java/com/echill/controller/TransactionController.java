package com.echill.controller;

import com.echill.dto.request.TransactionHistoryRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TransactionHistoryDto;
import com.echill.service.TransactionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionController {
    TransactionService transactionService;

    @GetMapping("/my-history")
    public ApiResponse<PageResponse<TransactionHistoryDto>> getMyTransactions(
            @Valid @ModelAttribute TransactionHistoryRequest request) {
        return ApiResponse.<PageResponse<TransactionHistoryDto>>builder()
                .data(transactionService.getMyTransactions(request))
                .build();
    }
}
