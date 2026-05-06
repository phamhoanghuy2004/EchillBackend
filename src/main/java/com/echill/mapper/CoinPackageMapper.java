package com.echill.mapper;

import com.echill.dto.request.CoinPackageCreateRequest;
import com.echill.dto.request.CoinPackageUpdateRequest;
import com.echill.dto.response.CoinPackageResponse;
import com.echill.entity.CoinPackage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoinPackageMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "bonusCoin", defaultValue = "0L")
    CoinPackage toEntity(CoinPackageCreateRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CoinPackageUpdateRequest request, @MappingTarget CoinPackage entity);

    CoinPackageResponse toResponse(CoinPackage entity);
}
