package com.echill.service;

import com.echill.dto.request.CoinPackageCreateRequest;
import com.echill.dto.request.CoinPackagePageRequest;
import com.echill.dto.request.CoinPackageUpdateRequest;
import com.echill.dto.response.CoinPackageResponse;
import com.echill.dto.response.PageResponse;
import com.echill.entity.CoinPackage;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.CoinPackageMapper;
import com.echill.repository.CoinPackageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoinPackageService {
    CoinPackageRepository coinPackageRepository;
    CoinPackageMapper coinPackageMapper;

    @Transactional
    public CoinPackageResponse createCoinPackage(CoinPackageCreateRequest request) {
        if (coinPackageRepository.existsByName(request.getName().trim())) {
            throw new AppException(ErrorEnum.COIN_PACKAGE_NAME_ALREADY_EXISTS);
        }

        if (request.getOriginalPrice() != null &&
                request.getOriginalPrice().compareTo(request.getPrice()) < 0) {
            throw new AppException(ErrorEnum.ORIGINAL_PRICE_LESS_THAN_SALE_PRICE);
        }

        CoinPackage newPackage = coinPackageMapper.toEntity(request);
        newPackage.setName(newPackage.getName().trim());

        newPackage = coinPackageRepository.save(newPackage);

        return coinPackageMapper.toResponse(newPackage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CoinPackageResponse> getActiveCoinPackages(CoinPackagePageRequest request) {
        Page<CoinPackage> pageData = coinPackageRepository.findAllByIsActiveTrue(request.getPageable());

        Page<CoinPackageResponse> responsePage = pageData.map(coinPackageMapper::toResponse);

        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CoinPackageResponse> getAllCoinPackages(CoinPackagePageRequest request) {
        Page<CoinPackage> pageData = coinPackageRepository.findAll(request.getPageable());

        Page<CoinPackageResponse> responsePage = pageData.map(coinPackageMapper::toResponse);

        return PageResponse.of(responsePage);
    }

    @Transactional
    public CoinPackageResponse updateCoinPackage(Long id, CoinPackageUpdateRequest request) {
        CoinPackage existingPackage = coinPackageRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.COIN_PACKAGE_NOT_FOUND));

        String newName = request.getName().trim();
        if (!existingPackage.getName().equalsIgnoreCase(newName) &&
                coinPackageRepository.existsByName(newName)) {
            throw new AppException(ErrorEnum.COIN_PACKAGE_NAME_ALREADY_EXISTS);
        }

        if (request.getOriginalPrice() != null &&
                request.getOriginalPrice().compareTo(request.getPrice()) < 0) {
            throw new AppException(ErrorEnum.ORIGINAL_PRICE_LESS_THAN_SALE_PRICE);
        }

        coinPackageMapper.updateEntityFromRequest(request, existingPackage);
        existingPackage.setName(newName);

        existingPackage = coinPackageRepository.save(existingPackage);
        return coinPackageMapper.toResponse(existingPackage);
    }

    @Transactional(readOnly = true)
    public CoinPackageResponse getCoinPackageById(Long id) {
        CoinPackage coinPackage = coinPackageRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.COIN_PACKAGE_NOT_FOUND));

        return coinPackageMapper.toResponse(coinPackage);
    }

}
