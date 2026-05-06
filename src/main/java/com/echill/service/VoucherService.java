package com.echill.service;

import com.echill.dto.request.VoucherCreationRequest;
import com.echill.dto.request.VoucherPageRequest;
import com.echill.dto.request.VoucherUpdateRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.VoucherResponse;
import com.echill.entity.User;
import com.echill.entity.Voucher;
import com.echill.entity.enums.DiscountType;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.VoucherMapper;
import com.echill.repository.UserRepository;
import com.echill.repository.VoucherRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VoucherService {
    VoucherRepository voucherRepository;
    VoucherMapper voucherMapper;
    UserRepository userRepository;

    @Transactional
    public VoucherResponse createVoucher(VoucherCreationRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase();
        request.setCode(normalizedCode);

        validateVoucherDates(request.getStartDate(), request.getEndDate());

        request.setMaxDiscountAmount(validateAndSanitizeDiscount(
                request.getDiscountType(), request.getDiscountValue(), request.getMaxDiscountAmount()
        ));

        if (voucherRepository.existsByCode(normalizedCode)) {
            log.warn("Attempt to create duplicate voucher code: {}", normalizedCode);
            throw new AppException(ErrorEnum.VOUCHER_CODE_EXISTED);
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Voucher voucher = voucherMapper.toEntity(request);

        User proxyUser = userRepository.getReferenceById(currentUserId);
        voucher.setCreator(proxyUser);

        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("Created voucher successfully with code: {}", savedVoucher.getCode());

        return voucherMapper.toResponse(savedVoucher);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherUpdateRequest request) {

        Voucher voucher = voucherRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorEnum.VOUCHER_NOT_FOUND));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!voucher.getCreator().getId().equals(currentUserId)) {
            log.warn("User {} cố tình sửa Voucher {} của người khác!", currentUserId, id);
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        validateVoucherDates(request.getStartDate(), request.getEndDate());

        request.setMaxDiscountAmount(validateAndSanitizeDiscount(
                request.getDiscountType(), request.getDiscountValue(), request.getMaxDiscountAmount()
        ));

        if (voucher.getUsedCount() > 0) {
            boolean isCoreValueChanged = request.getDiscountType() != voucher.getDiscountType()
                    || isBigDecimalChanged(request.getDiscountValue(), voucher.getDiscountValue())
                    || isBigDecimalChanged(request.getMaxDiscountAmount(), voucher.getMaxDiscountAmount())
                    || isBigDecimalChanged(request.getMinOrderValue(), voucher.getMinOrderValue())
                    || !java.util.Objects.equals(request.getMinCourseCount(), voucher.getMinCourseCount());

            if (isCoreValueChanged) {
                log.warn("Voucher {} đã có người dùng. Chặn hành vi đổi Core Value.", voucher.getCode());
                throw new AppException(ErrorEnum.VOUCHER_ALREADY_USED);
            }
        }

        voucherMapper.updateEntityFromRequest(request, voucher);
        Voucher updatedVoucher = voucherRepository.save(voucher);

        log.info("User {} đã cập nhật thành công Voucher: {}", currentUserId, voucher.getCode());

        return voucherMapper.toResponse(updatedVoucher);
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getMyVouchers(VoucherPageRequest request) {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        Pageable pageable = request.getPageable();

        Page<Voucher> voucherPage = voucherRepository.findVouchersByCreatorId(currentUserId, pageable);

        Page<VoucherResponse> responsePage = voucherPage.map(voucherMapper::toResponse);

        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getPublicVouchers() {
        List<Voucher> validVouchers = voucherRepository.findValidPublicVouchers(Instant.now());
        return validVouchers.stream()
                .map(voucherMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getActiveComboVouchers() {

        Instant now = Instant.now();
        List<Voucher> comboVouchers = voucherRepository.findActiveComboVouchers(now);

        return comboVouchers.stream()
                .map(voucherMapper::toResponse)
                .toList();
    }

    // =========================================
    // 🛠️ HELPER METHODS (GOM LOGIC TRÙNG LẶP)
    // =========================================

    private void validateVoucherDates(Instant startDate, Instant endDate) {
        if (endDate.compareTo(startDate) <= 0) {
            throw new AppException(ErrorEnum.VOUCHER_DATE_INVALID);
        }
    }

    private BigDecimal validateAndSanitizeDiscount(DiscountType type, BigDecimal value, BigDecimal maxAmount) {
        if (type == DiscountType.PERCENT) {
            if (value.compareTo(new BigDecimal("100")) > 0) {
                throw new AppException(ErrorEnum.VOUCHER_PERCENT_INVALID);
            }
            if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorEnum.VOUCHER_MAX_DISCOUNT_REQUIRED);
            }
            return maxAmount;
        } else if (type == DiscountType.VALUE) {
            return null;
        }
        return maxAmount;
    }

    private boolean isBigDecimalChanged(BigDecimal val1, BigDecimal val2) {
        if (val1 == null && val2 == null) return false;
        if (val1 == null || val2 == null) return true;
        return val1.compareTo(val2) != 0;
    }
}