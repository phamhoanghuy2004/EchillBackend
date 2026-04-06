package com.echill.service;

import com.echill.config.VnpayConfig;
import com.echill.constant.VnpayConstant;
import com.echill.dto.response.VnpayIpnResponse;
import com.echill.entity.Course;
import com.echill.entity.Transaction;
import com.echill.entity.TransactionItem;
import com.echill.entity.User;
import com.echill.entity.enums.Status;
import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;
import com.echill.event.TransactionSuccessEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {
    TransactionRepository transactionRepository;
    VnpayConfig vnpayConfig;
    ApplicationEventPublisher eventPublisher;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    UserRepository userRepository;

    private static final String VNPAY_ORDER_TYPE = "other";
    private static final String CURRENCY_VND = "VND";
    private static final String TIME_ZONE = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public String initiateCoursePayment(Long courseId, HttpServletRequest request) {

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Course course = courseRepository.findByIdAndStatus(courseId, Status.ACTIVE)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("CRITICAL: Khóa học {} có giá trị không hợp lệ (Null hoặc <= 0)!", courseId);
            throw new AppException(ErrorEnum.INVALID_AMOUNT);
        }

        if (enrollmentRepository.existsByStudentAndCourse(currentUser, course)) {
            log.warn("User {} cố tình mua lại khóa học {} đã sở hữu!", currentUser.getId(), courseId);
            throw new AppException(StudentErrorEnum.ALREADY_OWNED_COURSE);
        }

        Optional<Transaction> existingPendingTxn = transactionRepository
                .findPendingTransactionByUserAndCourseForUpdate(currentUser.getId(), courseId);

        if (existingPendingTxn.isPresent()) {
            Transaction pendingTxn = existingPendingTxn.get();
            log.info("♻️ [TÁI SỬ DỤNG] User {} bấm thanh toán nhiều lần. Dùng lại Transaction PENDING: {}",
                    currentUser.getId(), pendingTxn.getTransactionCode());
            return createVnpayPaymentUrl(pendingTxn, request);
        }

        log.info("Tạo Transaction mới cho User {} mua Course {}", currentUser.getId(), courseId);


        String txnCode = "TXN_" + TsidCreator.getTsid();

        Transaction newTransaction = Transaction.builder()
                .transactionCode(txnCode)
                .user(currentUser)
                .status(TransactionStatus.PENDING)
                .type(TransactionType.VNPAY)
                .totalAmount(course.getPrice())
                .totalCoinsChanged(0L)
                .expiredAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        TransactionItem item = TransactionItem.builder()
                .course(course)
                .amountPrice(course.getPrice())
                .build();

        newTransaction.addItem(item);

        transactionRepository.save(newTransaction);

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

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TIME_ZONE));
        vnp_Params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMATTER));
        vnp_Params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_DATE_FORMATTER));

        if (log.isDebugEnabled()) {
            log.debug("VNPAY Params gửi đi: {}", vnp_Params);
        }

        String queryUrl = VnpayUtil.buildQueryUrl(vnp_Params);
        String vnp_SecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), queryUrl);

        String paymentUrl = vnpayConfig.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

        log.info("Tạo URL VNPAY thành công cho mã GD: {}", transaction.getTransactionCode());

        return paymentUrl;
    }

    @Transactional
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

        if ("00".equals(responseCode)) {
            Long currentBalance = transaction.getUser().getCurrentCoin();
            transaction.setVnpTransactionNo(vnp_TransactionNo);
            transaction.markAsSuccess(currentBalance);
            eventPublisher.publishEvent(new TransactionSuccessEvent(transaction.getId()));
        } else {
            transaction.markAsFailed();
            log.info("Khách hàng hủy hoặc thanh toán thất bại đơn: {}", transactionCode);
        }

        return new VnpayIpnResponse(VnpayConstant.RSP_SUCCESS, VnpayConstant.MSG_SUCCESS);
    }
}
