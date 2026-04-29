package com.echill.service;

import com.echill.config.VnpayConfig;
import com.echill.constant.VnpayConstant;
import com.echill.dto.response.VnpayIpnResponse;
import com.echill.entity.*;
import com.echill.entity.enums.DiscountType;
import com.echill.entity.enums.EnrollmentStatus;
import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.*;
import com.echill.util.SecurityUtils;
import com.echill.util.VnpayUtil;
import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {
    TransactionRepository transactionRepository;
    VnpayConfig vnpayConfig;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    UserRepository userRepository;
    VoucherRepository voucherRepository;

    private static final String VNPAY_ORDER_TYPE = "other";
    private static final String CURRENCY_VND = "VND";
    private static final String TIME_ZONE = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public String initiatePayment(List<Long> courseIds, String voucherCode, HttpServletRequest request) {
        // 1. Authenticate & Fetch Data
        User currentUser = getCurrentUserSecurely();
        List<Course> courses = validateAndFetchCourses(courseIds);
        validateOwnership(currentUser, courseIds);

        // 2. Calculate Base Money
        BigDecimal totalOriginalPrice = calculateTotalOriginalPrice(courses);

        // 3. Handle Voucher Policy (Atomic Lock)
        Voucher voucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            voucher = voucherRepository.findVoucherByCode(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorEnum.VOUCHER_NOT_FOUND));

            voucher.validateApplicability(totalOriginalPrice, courses.size());

            discountAmount = voucher.calculateDiscount(totalOriginalPrice);
        }

        // 4. Final Money
        BigDecimal finalPayablePrice = totalOriginalPrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // 5. Handle Pending Transactions (Clean up & Release side effects)
        Transaction matchingPendingTxn = resolvePendingTransactions(currentUser, courseIds, voucher);
        if (matchingPendingTxn != null) {
            log.info("[PAYMENT_REUSED] UserId: {}, TxnCode: {}", currentUser.getId(), matchingPendingTxn.getTransactionCode());
            return createVnpayPaymentUrl(matchingPendingTxn, request);
        }

        if (voucher != null) {
            int updatedRows = voucherRepository.atomicReserveVoucherSlot(voucher.getCode());
            if (updatedRows == 0) {
                throw new AppException(ErrorEnum.VOUCHER_USAGE_LIMIT_EXCEEDED);
            }
        }

        // 6. Create New Transaction
        Transaction newTransaction = createNewTransaction(currentUser, courses, voucher, discountAmount, finalPayablePrice);

        // 7. Audit Log Cấp Production
        log.info("[PAYMENT_INITIATED] UserId: {}, TxnCode: {}, CourseIds: {}, Voucher: {}, BasePrice: {}, Discount: {}, FinalPrice: {}, IP: {}",
                currentUser.getId(), newTransaction.getTransactionCode(), courseIds, voucherCode, totalOriginalPrice, discountAmount, finalPayablePrice, request.getRemoteAddr());

        return createVnpayPaymentUrl(newTransaction, request);
    }

    // =========================================================================================
    // 🛠️ PRIVATE HELPER METHODS (Đọc code như đọc truyện)
    // =========================================================================================

    private User getCurrentUserSecurely() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
    }

    private List<Course> validateAndFetchCourses(List<Long> courseIds) {
        List<Course> courses = courseRepository.findAllActiveByIds(courseIds);
        if (courses.size() != courseIds.size()) {
            throw new AppException(TeacherErrorEnum.COURSE_NOT_FOUND);
        }
        for (Course course : courses) {
            if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorEnum.INVALID_AMOUNT);
            }
        }
        return courses;
    }

    private void validateOwnership(User user, List<Long> courseIds) {
        List<Long> ownedIds = enrollmentRepository.findOwnedCourseIds(user.getId(), courseIds);
        if (!ownedIds.isEmpty()) {
            throw new AppException(StudentErrorEnum.ALREADY_OWNED_COURSE);
        }
    }

    private BigDecimal calculateTotalOriginalPrice(List<Course> courses) {
        return courses.stream()
                .map(Course::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Transaction resolvePendingTransactions(User user, List<Long> courseIds, Voucher requestedVoucher) {
        List<Transaction> pendingTxns = transactionRepository.findAllPendingTransactionsByUser(user.getId());
        Transaction exactMatch = null;
        Set<Long> requestCourseIdSet = new HashSet<>(courseIds);

        for (Transaction pending : pendingTxns) {
            Set<Long> pendingCourseIdSet = pending.getItems().stream()
                    .map(item -> item.getCourse().getId())
                    .collect(Collectors.toSet());

            boolean isSameCourses = pendingCourseIdSet.equals(requestCourseIdSet);
            boolean isSameVoucher = (requestedVoucher == null && pending.getVoucher() == null) ||
                    (requestedVoucher != null && pending.getVoucher() != null && requestedVoucher.getId().equals(pending.getVoucher().getId()));

            if (isSameCourses && isSameVoucher && pending.getExpiredAt() != null && pending.getExpiredAt().isAfter(Instant.now())) {
                exactMatch = pending;
            } else {
                cancelTransactionAndRollbackVoucherSlot(pending);
            }
        }
        return exactMatch;
    }

    private void cancelTransactionAndRollbackVoucherSlot(Transaction pendingTxn) {
        pendingTxn.markAsFailed();
        if (pendingTxn.getVoucher() != null) {
            voucherRepository.atomicReleaseVoucherSlot(pendingTxn.getVoucher().getCode());
        }
    }

    private Transaction createNewTransaction(User user, List<Course> courses, Voucher voucher,
                                             BigDecimal discount, BigDecimal finalPrice) {

        String txnCode = "TXN_" + TsidCreator.getTsid();
        Instant now = Instant.now();

        Transaction txn = Transaction.builder()
                .transactionCode(txnCode)
                .user(user)
                .status(TransactionStatus.PENDING)
                .type(TransactionType.VNPAY)
                .totalAmount(finalPrice)
                .voucher(voucher)
                .discountAmount(discount)
                .totalCoinsChanged(0L)
                .createdAt(now)
                .expiredAt(now.plus(15, ChronoUnit.MINUTES))
                .description(courses.size() == 1 ? "Thanh toan khoa hoc" : "Thanh toan combo")
                .build();

        for (Course course : courses) {
            TransactionItem item = TransactionItem.builder()
                    .course(course)
                    .amountPrice(course.getPrice())
                    .build();
            txn.addItem(item);
        }

        return transactionRepository.saveAndFlush(txn);
    }

    public String createVnpayPaymentUrl(Transaction transaction, HttpServletRequest request) {
        log.info("Bắt đầu tạo URL thanh toán VNPAY cho Transaction ID: {}", transaction.getTransactionCode());

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Giao dịch {} không hợp lệ để thanh toán (Status: {})",
                    transaction.getTransactionCode(), transaction.getStatus());
            throw new AppException(ErrorEnum.TRANSACTION_INVALID_STATUS);
        }

        if (transaction.getTotalAmount() == null || transaction.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Số tiền <= 0 cho Transaction: {}", transaction.getTransactionCode());
            throw new AppException(ErrorEnum.INVALID_AMOUNT);
        }

        long amount;
        try {
            amount = transaction.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
        } catch (ArithmeticException e) {
            log.error("Lỗi tràn số khi tính tiền cho Transaction: {}", transaction.getTransactionCode(), e);
            throw new AppException(ErrorEnum.INVALID_AMOUNT);
        }

        // 3. Build Base Params
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnp_Params.put("vnp_CurrCode", CURRENCY_VND);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_TxnRef", transaction.getTransactionCode());
        vnp_Params.put("vnp_OrderInfo", "Thanh toan khoa hoc: " + transaction.getTransactionCode());
        vnp_Params.put("vnp_OrderType", VNPAY_ORDER_TYPE);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnpayUtil.getIpAddress(request));

        ZonedDateTime createDate = ZonedDateTime.ofInstant(transaction.getCreatedAt(), ZoneId.of(TIME_ZONE));
        ZonedDateTime expireDate = ZonedDateTime.ofInstant(transaction.getExpiredAt(), ZoneId.of(TIME_ZONE));

        vnp_Params.put("vnp_CreateDate", createDate.format(VNPAY_DATE_FORMATTER));
        vnp_Params.put("vnp_ExpireDate", expireDate.format(VNPAY_DATE_FORMATTER));

        if (log.isDebugEnabled()) {
            log.debug("VNPAY Params gửi đi: {}", vnp_Params);
        }

        String queryUrl = VnpayUtil.buildQueryUrl(vnp_Params);
        String vnp_SecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), queryUrl);

        String paymentUrl = vnpayConfig.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

        log.info("Tạo URL VNPAY thành công cho mã GD: {}", transaction.getTransactionCode());

        return paymentUrl;
    }

    @Transactional(rollbackFor = Exception.class)
    public VnpayIpnResponse processIpnWebhook(Map<String, String> fields) {
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        String transactionCode = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String amountStr = fields.get("vnp_Amount");
        String tmnCode = fields.get("vnp_TmnCode");
        String vnp_TransactionNo = fields.get("vnp_TransactionNo");

        if (transactionCode == null || responseCode == null || amountStr == null || vnp_SecureHash == null) {
            log.error("🚨 [VNPAY IPN] Thiếu tham số bắt buộc!");
            return new VnpayIpnResponse(VnpayConstant.RSP_UNKNOWN_ERROR, VnpayConstant.MSG_UNKNOWN_ERROR);
        }

        if (!vnpayConfig.getTmnCode().equals(tmnCode)) {
            log.error("🚨 [VNPAY IPN] TmnCode không hợp lệ: {}", tmnCode);
            return new VnpayIpnResponse(VnpayConstant.RSP_INVALID_SIGNATURE, VnpayConstant.MSG_INVALID_SIGNATURE);
        }

        if (!VnpayUtil.verifySignature(fields, vnp_SecureHash, vnpayConfig.getHashSecret())) {
            log.error("🚨 [VNPAY IPN] Sai chữ ký bảo mật cho đơn: {}", transactionCode);
            return new VnpayIpnResponse(VnpayConstant.RSP_INVALID_SIGNATURE, VnpayConstant.MSG_INVALID_SIGNATURE);
        }

        long vnpAmount;
        try {
            vnpAmount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            return new VnpayIpnResponse(VnpayConstant.RSP_INVALID_AMOUNT, VnpayConstant.MSG_INVALID_AMOUNT);
        }

        Transaction transaction = transactionRepository.findByTransactionCodeForUpdate(transactionCode)
                .orElse(null);

        if (transaction == null) {
            return new VnpayIpnResponse(VnpayConstant.RSP_ORDER_NOT_FOUND, VnpayConstant.MSG_ORDER_NOT_FOUND);
        }

        long dbAmount = transaction.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        if (vnpAmount != dbAmount) {
            log.error("🚨 [VNPAY IPN] LỆCH TIỀN! VNPAY: {}, DB: {}", vnpAmount, dbAmount);
            return new VnpayIpnResponse(VnpayConstant.RSP_INVALID_AMOUNT, VnpayConstant.MSG_INVALID_AMOUNT);
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("ℹ️ [VNPAY IPN] Trùng lặp IPN (Đã xử lý trước đó). Bỏ qua đơn: {}", transactionCode);
            return new VnpayIpnResponse(VnpayConstant.RSP_ALREADY_CONFIRMED, VnpayConstant.MSG_ALREADY_CONFIRMED);
        }

        transaction.setVnpTransactionNo(vnp_TransactionNo);

        if ("00".equals(responseCode)) {
            Long currentBalance = transaction.getUser().getCurrentCoin();
            transaction.markAsSuccess(currentBalance);
            grantCoursesToUser(transaction);
        } else {
            transaction.markAsFailed();
            log.info("Khách hàng hủy hoặc thanh toán thất bại đơn: {}", transactionCode);

            if (transaction.getVoucher() != null) {
                voucherRepository.atomicReleaseVoucherSlot(transaction.getVoucher().getCode());
                log.info("Đã hoàn trả 1 lượt sử dụng cho mã Voucher: {}", transaction.getVoucher().getCode());
            }
        }

        transactionRepository.saveAndFlush(transaction);

        return new VnpayIpnResponse(VnpayConstant.RSP_SUCCESS, VnpayConstant.MSG_SUCCESS);
    }

    private void grantCoursesToUser(Transaction transaction) {
        User student = transaction.getUser();
        List<TransactionItem> items = transaction.getItems();

        if (items == null || items.isEmpty()) {
            log.warn("⚠️ BỎ QUA [Txn: {}] - Hóa đơn không có TransactionItem.", transaction.getId());
            return;
        }

        Set<Long> existingCourseIds = enrollmentRepository.findCourseIdsByStudentId(student.getId());
        List<Enrollment> newEnrollments = new ArrayList<>();

        for (TransactionItem item : items) {
            Course course = item.getCourse();
            if (course == null) continue;

            if (existingCourseIds.contains(course.getId())) {
                continue;
            }

            Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .course(course)
                    .enrollmentStatus(EnrollmentStatus.ACTIVE)
                    .build();
            newEnrollments.add(enrollment);
        }

        if (!newEnrollments.isEmpty()) {
            enrollmentRepository.saveAll(newEnrollments);
            log.info("🎉 Đã cấp thành công {} khóa học cho User {}", newEnrollments.size(), student.getId());
        }
    }
}
