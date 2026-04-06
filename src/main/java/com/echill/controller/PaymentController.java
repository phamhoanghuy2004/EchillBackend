package com.echill.controller;

import com.echill.constant.VnpayConstant;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.VnpayIpnResponse;
import com.echill.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentController {
    PaymentService paymentService;

    @PostMapping("/checkout/course/{courseId}")
    public ApiResponse<String> checkoutCourse(
            @PathVariable("courseId") Long courseId,
            HttpServletRequest request) {

        log.info("🛒 Nhận request tạo URL thanh toán cho khóa học: {}", courseId);

        String paymentUrl = paymentService.initiateCoursePayment(courseId, request);

        return ApiResponse.<String>builder()
                        .message("Tạo URL thanh toán VNPAY thành công")
                        .data(paymentUrl)
                        .build();
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<VnpayIpnResponse> vnpayIPN(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                fields.put(fieldName, fieldValue);
            }
        }

        log.info("📬 [VNPAY IPN] Nhận payload: {}", fields);

        try {
            VnpayIpnResponse response = paymentService.processIpnWebhook(fields);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🚨 [VNPAY IPN] Lỗi hệ thống nghiêm trọng: ", e);
            return ResponseEntity.ok(new VnpayIpnResponse(VnpayConstant.RSP_UNKNOWN_ERROR, VnpayConstant.MSG_UNKNOWN_ERROR));
        }
    }

}
