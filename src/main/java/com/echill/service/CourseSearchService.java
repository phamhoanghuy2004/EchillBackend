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

    private static final List<Level> BEGINNER_PATH =
            List.of(Level.BEGINNER, Level.INTERMEDIATE, Level.ADVANCED);

    private static final List<Level> INTERMEDIATE_PATH =
            List.of(Level.INTERMEDIATE, Level.ADVANCED);

    private static final List<Level> ADVANCED_PATH =
            List.of(Level.ADVANCED);

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

//    public List<CourseCardResponse> getRecommendedComboForCurrentUser() {
//
//        Long userId = SecurityUtils.getCurrentUserId();
//        log.info("🎯 Bắt đầu trích xuất hồ sơ năng lực để đề xuất lộ trình cho User: {}", userId);
//
//        Level currentLevel = studentProfileRepository.findByUserId(userId)
//                .map(StudentProfile::getLevel)
//                .orElse(Level.UNDETERMINED);
//
//        List<UserSkillProfile> userSkills = userSkillProfileRepository.findByUserIdAndTagGroup(userId, TagGroup.ENGLISH_TOEIC);
//
//        Map<Long, Double> tagProficiencies = userSkills.stream()
//                .collect(Collectors.toMap(
//                        p -> p.getTag().getId(),
//                        UserSkillProfile::getProficiencyPercentage
//                ));
//
//        return suggestComboPathForUser(currentLevel, tagProficiencies);
//    }

    public List<CourseCardResponse> suggestComboPathForUser(Level currentLevel, Map<Long, Double> tagProficiencies) {
        log.info("🔍 [MSEARCH] Tìm lộ trình cho Level {} với bản đồ năng lực (tagIds): {}", currentLevel, tagProficiencies);

        List<CourseCardResponse> recommendedPath = new ArrayList<>();
        List<Level> targetLevels = getNextLevels(currentLevel);

        if (targetLevels.isEmpty()) return recommendedPath;

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> baseShouldQueries = new ArrayList<>();

        if (tagProficiencies != null && !tagProficiencies.isEmpty()) {
            for (Map.Entry<Long, Double> entry : tagProficiencies.entrySet()) {
                float boostWeight = (float) (100.0 - entry.getValue());
                if (boostWeight > 10.0f) {
                    baseShouldQueries.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.term(t -> t
                            .field("tagIds")
                            .value(entry.getKey())
                            .boost(boostWeight)
                    )));
                }
            }
        }

        List<org.springframework.data.elasticsearch.core.query.Query> multiQueries = new ArrayList<>();

        for (Level targetLvl : targetLevels) {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            boolQuery.filter(f -> f.term(t -> t.field("status").value(Status.ACTIVE.name())));
            boolQuery.filter(f -> f.term(t -> t.field("level").value(targetLvl.name())));

            if (!baseShouldQueries.isEmpty()) {
                boolQuery.should(baseShouldQueries);
                boolQuery.minimumShouldMatch("0");
            }

            NativeQueryBuilder queryBuilder = NativeQuery.builder()
                    .withQuery(boolQuery.build()._toQuery())
                    .withPageable(PageRequest.of(0, 1))
                    .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));

            multiQueries.add(queryBuilder.build());
        }


        List<SearchHits<CourseDocument>> multiSearchHits =
                elasticsearchOperations.multiSearch(multiQueries, CourseDocument.class);

        for (int i = 0; i < multiSearchHits.size(); i++) {
            SearchHits<CourseDocument> hits = multiSearchHits.get(i);
            Level lvl = targetLevels.get(i);

            if (hits.hasSearchHits()) {
                CourseDocument bestMatchCourse = hits.getSearchHits().getFirst().getContent();
                recommendedPath.add(courseDocumentMapper.toResponse(bestMatchCourse));
            } else {
                log.warn("⚠️ Không tìm thấy khóa học ACTIVE nào phù hợp cho Level: {}", lvl);
            }
        }

        enrichWithStats(recommendedPath);

        return recommendedPath;
    }

    private List<Level> getNextLevels(Level currentLevel) {
        if (currentLevel == null) {
            return BEGINNER_PATH;
        }

        return switch (currentLevel) {
            case UNDETERMINED, BEGINNER -> BEGINNER_PATH;
            case INTERMEDIATE -> INTERMEDIATE_PATH;
            case ADVANCED -> ADVANCED_PATH;
        };
    }
}