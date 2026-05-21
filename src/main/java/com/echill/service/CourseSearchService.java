package com.echill.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.echill.document.CourseDocument;
import com.echill.dto.request.elasticsearch.request.CourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.CourseSortType;
import com.echill.entity.enums.Level;
import com.echill.entity.enums.Status;
import com.echill.entity.enums.TagGroup;
import com.echill.mapper.document.CourseDocumentMapper;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.ReviewRepository;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseSearchService {
    ElasticsearchOperations elasticsearchOperations;
    CourseDocumentMapper courseDocumentMapper;
    StudentProfileRepository studentProfileRepository;
    UserSkillProfileRepository userSkillProfileRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;

    public Page<CourseCardResponse> searchCourses(CourseSearchRequest request) {

        log.info("Thực thi tìm kiếm khóa học với request: {}", request);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        filterQueries.add(Query.of(q -> q.term(t -> t.field("status").value(Status.ACTIVE.name()))));

        if (request.getCategoryId() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("categoryId").value(request.getCategoryId()))));
        }

        if (request.getLevel() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("level").value(request.getLevel().name()))));
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> {
                r.field("price");
                if (request.getMinPrice() != null) r.gte(JsonData.of(request.getMinPrice()));
                if (request.getMaxPrice() != null) r.lte(JsonData.of(request.getMaxPrice()));
                return r;
            })));
        }

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim();

            mustQueries.add(Query.of(q -> q.multiMatch(m -> m
                    // Dấu ^3 nghĩa là: Nếu từ khóa khớp với Tên khóa học,
                    .fields(List.of("name^3", "teacherName^2"))
                    .query(keyword)
                    .fuzziness("AUTO")
            )));
        }

        if (!mustQueries.isEmpty()) {
            boolQueryBuilder.must(mustQueries);
        }
        if (!filterQueries.isEmpty()) {
            boolQueryBuilder.filter(filterQueries);
        }

        PageRequest pageable = PageRequest.of(request.getSafePage(), request.getSafeSize());
        NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withPageable(pageable);

        CourseSortType actualSort = request.getSortBy();

        // Nếu User không truyền sortBy, Backend sẽ tự đoán ý:
        if (actualSort == null) {
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                actualSort = CourseSortType.RELEVANCE; // Có gõ chữ -> Ưu tiên độ chính xác
            } else {
                actualSort = CourseSortType.NEWEST;    // Lướt dạo -> Ưu tiên hàng mới
            }
        }

        // Áp dụng thuật toán sắp xếp
        switch (actualSort) {
            case PRICE_ASC:
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc).missing(FieldValue.of("_last"))));
                break;
            case PRICE_DESC:
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc).missing(FieldValue.of("_last"))));
                break;
            case NEWEST:
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc).missing(FieldValue.of("_last"))));
                break;
            case RELEVANCE:
            default:
                // 💥 KHI SẮP XẾP THEO RELEVANCE:
                // Chỉ cần sort theo _score (Điểm do ES tự chấm dựa trên Fuzziness và Boosting)
                nativeQueryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                break;
        }

        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(nativeQueryBuilder.build(), CourseDocument.class);

        List<CourseCardResponse> responses = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(courseDocumentMapper::toResponse)
                .toList();

        enrichWithStats(responses);

        log.info(responses.toString());

        return new PageImpl<>(responses, pageable, searchHits.getTotalHits());
    }

    private void enrichWithStats(List<CourseCardResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<Long> courseIds = responses.stream().map(CourseCardResponse::getId).toList();

        Map<Long, Long> studentCounts = enrollmentRepository.countEnrollmentsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Long> reviewCounts = reviewRepository.countReviewsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Double> averageRatings = reviewRepository.getAverageRatingsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Double) arr[1]));

        responses.forEach(card -> {
            card.setStudentCount(studentCounts.getOrDefault(card.getId(), 0L));
            card.setReviewCount(reviewCounts.getOrDefault(card.getId(), 0L));
            Double avg = averageRatings.get(card.getId());
            card.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        });
    }

    public List<CourseCardResponse> getRecommendedComboForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("🎯 Bắt đầu trích xuất hồ sơ năng lực để đề xuất 3 khóa học cho User: {}", userId);

        Level currentLevel = studentProfileRepository.findByUserId(userId)
                .map(StudentProfile::getLevel)
                .orElse(Level.UNDETERMINED);

        List<UserSkillProfile> userChildSkills = userSkillProfileRepository.findChildProfilesByUserIdAndTagGroup(userId, TagGroup.ENGLISH_TOEIC);

        Map<Long, Float> tagBoostWeights = userChildSkills.stream()
                .filter(p -> p.getTag().getMaxLevel() != null && p.getTag().getMaxLevel() > 0)
                .collect(Collectors.toMap(
                        p -> p.getTag().getId(),
                        p -> {
                            int cappedLevel = Math.min(p.getCurrentLevel(), p.getTag().getMaxLevel());
                            float masteryRatio = (float) cappedLevel / p.getTag().getMaxLevel();
                            return (1.0f - masteryRatio) * 100.0f;
                        }
                ));

        return suggestComboPathForUser(currentLevel, tagBoostWeights);
    }

    public List<CourseCardResponse> suggestComboPathForUser(Level currentLevel, Map<Long, Float> tagBoostWeights) {
        Level targetLevel = (currentLevel == null || currentLevel == Level.UNDETERMINED)
                ? Level.BEGINNER
                : currentLevel;

        log.info("🔍 [SEARCH] Tìm Top 3 khóa học cho Level {} với Bản đồ lỗ hổng năng lực: {}", targetLevel, tagBoostWeights);

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        boolQuery.filter(f -> f.term(t -> t.field("status").value(Status.ACTIVE.name())));
        boolQuery.filter(f -> f.term(t -> t.field("level").value(targetLevel.name())));

        if (tagBoostWeights != null && !tagBoostWeights.isEmpty()) {
            List<co.elastic.clients.elasticsearch._types.query_dsl.Query> baseShouldQueries = new ArrayList<>();
            for (Map.Entry<Long, Float> entry : tagBoostWeights.entrySet()) {
                float boostWeight = entry.getValue();

                if (boostWeight > 25.0f) {
                    baseShouldQueries.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.term(t -> t
                            .field("tagIds")
                            .value(entry.getKey())
                            .boost(boostWeight)
                    )));
                }
            }

            if (!baseShouldQueries.isEmpty()) {
                boolQuery.should(baseShouldQueries);
                boolQuery.minimumShouldMatch("0");
            }
        }

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(PageRequest.of(0, 3))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));

        SearchHits<CourseDocument> hits = elasticsearchOperations.search(queryBuilder.build(), CourseDocument.class);

        if (!hits.hasSearchHits()) {
            log.warn("⚠️ Không tìm thấy khóa học ACTIVE nào phù hợp cho Level: {}", targetLevel);
            return new ArrayList<>();
        }

        List<CourseCardResponse> recommendedPath = hits.getSearchHits().stream()
                .map(hit -> courseDocumentMapper.toResponse(hit.getContent()))
                .collect(Collectors.toList());

        enrichWithStats(recommendedPath);

        return recommendedPath;
    }
}