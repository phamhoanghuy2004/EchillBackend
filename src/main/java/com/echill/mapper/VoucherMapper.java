package com.echill.mapper;

import com.echill.dto.request.VoucherCreationRequest;
import com.echill.dto.request.VoucherUpdateRequest;
import com.echill.dto.response.VoucherResponse;
import com.echill.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    @Mapping(target = "usedCount", constant = "0")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isAutoApplied", source = "isAutoApplied", defaultValue = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", ignore = true)
    Voucher toEntity(VoucherCreationRequest request);

    VoucherResponse toResponse(Voucher voucher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "creator", ignore = true)
    void updateEntityFromRequest(VoucherUpdateRequest request, @MappingTarget Voucher voucher);
}
