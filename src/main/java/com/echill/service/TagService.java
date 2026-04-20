package com.echill.service;

import com.echill.dto.response.TagResponse;
import com.echill.mapper.TagMapper;
import com.echill.repository.TagRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TagService {
    TagRepository tagRepository;
    TagMapper tagMapper;

    @Cacheable(value = "tags", sync = true)
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        log.info("🚀 [CACHE MISS] Đang chui xuống Database để móc danh sách Tags...");

        return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
}
