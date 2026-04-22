package com.echill.service;

import com.echill.config.VnpayConfig;
import com.echill.constant.VnpayConstant;
import com.echill.dto.response.VnpayIpnResponse;
import com.echill.entity.*;
import com.echill.entity.enums.EnrollmentStatus;
import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.TransactionRepository;
import com.echill.repository.UserRepository;
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

    private static final String VNPAY_ORDER_TYPE = "other";
    private static final String CURRENCY_VND = "VND";
    private static final String TIME_ZONE = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public String initiatePayment(List<Long> courseIds, HttpServletRequest request) {

        User currentUser = userRepository.findByIdWithLock(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        List<Course> courses = courseRepository.findAllActiveByIds(courseIds);
        if (courses.size() != courseIds.size()) {
            List<Long> foundIds = courses.stream().map(Course::getId).toList();
            List<Long> missingIds = courseIds.stream().filter(id -> !foundIds.contains(id)).toList();

            String missingIdsStr = missingIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            String errorMessage = "Lỗi: Các khóa học có ID [" + missingIdsStr + "] không tồn tại hoặc đã bị ẩn, Vui lòng bỏ chọn.";

            log.error("Khóa học không tồn tại: {}", missingIdsStr);
            throw new AppException(TeacherErrorEnum.COURSE_NOT_FOUND, errorMessage);
        }

        List<Long> ownedIds = enrollmentRepository.findOwnedCourseIds(currentUser.getId(), courseIds);
        if (!ownedIds.isEmpty()) {
            String ownedIdsStr = ownedIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            String errorMessage = "Bạn đã sở hữu các khóa học có ID [" + ownedIdsStr + "]. Vui lòng bỏ chọn các khóa này để tiếp tục thanh toán.";

            log.warn("User {} mua lộ trình chứa khóa đã sở hữu: {}", currentUser.getId(), ownedIdsStr);
            throw new AppException(StudentErrorEnum.ALREADY_OWNED_COURSE, errorMessage);
        }

        for (Course course : courses) {
            if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorEnum.INVALID_AMOUNT);
            }
        }

        List<Transaction> pendingTxns = transactionRepository.findAllPendingTransactionsByUser(currentUser.getId());
        Transaction exactMatchTxn = null;

        Set<Long> requestCourseIdSet = new HashSet<>(courseIds);

        for (Transaction pending : pendingTxns) {
            Set<Long> pendingCourseIdSet = pending.getItems().stream()
                    .map(item -> item.getCourse().getId())
                    .collect(Collectors.toSet());

            if (pendingCourseIdSet.equals(requestCourseIdSet)) {
                if (pending.getExpiredAt() != null && pending.getExpiredAt().isAfter(Instant.now())) {
                    exactMatchTxn = pending;
                } else {
                    log.info("Transaction {} khớp giỏ hàng nhưng đã HẾT HẠN -> Hủy", pending.getTransactionCode());
                    pending.setStatus(TransactionStatus.FAILED);
                }
            } else {
                log.info("Transaction {} lệch giỏ hàng -> Hủy", pending.getTransactionCode());
                pending.setStatus(TransactionStatus.FAILED);
            }
        }

        if (exactMatchTxn != null) {
            log.info("♻️ [TÁI SỬ DỤNG] User {} spam click. Trả về TXN cũ: {}", currentUser.getId(), exactMatchTxn.getTransactionCode());
            return createVnpayPaymentUrl(exactMatchTxn, request);
        }


        BigDecimal totalOriginalAmount = courses.stream()
                .map(Course::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;

        log.info("Tạo Transaction mới cho Combo {} của User {}", courseIds, currentUser.getId());
        String txnCode = "TXN_" + TsidCreator.getTsid();
        Instant now = Instant.now();

        Transaction newTransaction = Transaction.builder()
                .transactionCode(txnCode)
                .user(currentUser)
                .status(TransactionStatus.PENDING)
                .type(TransactionType.VNPAY)
                .totalAmount(totalOriginalAmount)
                .discountAmount(discountAmount)
                .totalCoinsChanged(0L)
                .createdAt(now)
                .expiredAt(now.plus(15, ChronoUnit.MINUTES))
                .description(courses.size() == 1
                        ? "Thanh toan khoa hoc " + courses.getFirst().getId()
                        : "Thanh toan combo " + courses.size() + " khoa hoc")
                .build();

        for (Course course : courses) {
            TransactionItem item = TransactionItem.builder()
                    .course(course)
                    .amountPrice(course.getPrice())
                    .build();
            newTransaction.addItem(item);
        }

        newTransaction = transactionRepository.saveAndFlush(newTransaction);

        return createVnpayPaymentUrl(newTransaction, request);
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
