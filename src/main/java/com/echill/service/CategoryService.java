package com.echill.service;

import com.echill.constant.CacheNames;
import com.echill.dto.response.CategoryResponse;
import com.echill.entity.enums.Status;
import com.echill.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "'all_active'", sync = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByStatus(Status.ACTIVE).stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
