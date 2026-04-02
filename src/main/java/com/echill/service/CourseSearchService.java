package com.echill.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.echill.document.CourseDocument;
import com.echill.dto.request.elasticsearch.request.CourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.entity.enums.CourseSortType;
import com.echill.entity.enums.Status;
import com.echill.mapper.document.CourseDocumentMapper;
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

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseSearchService {
    ElasticsearchOperations elasticsearchOperations;
    CourseDocumentMapper courseDocumentMapper;

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

        log.info(responses.toString());

        return new PageImpl<>(responses, pageable, searchHits.getTotalHits());
    }
}