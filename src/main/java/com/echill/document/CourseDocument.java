package com.echill.document;

import com.echill.entity.enums.Level;
import com.echill.entity.enums.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "idx_courses_v2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDocument {

    @Id
    Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    String name;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    BigDecimal price;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100, index = false)
    BigDecimal originalPrice;

    @Field(type = FieldType.Integer, index = false)
    Integer discountPercent;

    @Field(type = FieldType.Keyword, index = false)
    String imageUrl;

    @Field(type = FieldType.Keyword)
    Level level;

    @Field(type = FieldType.Long)
    Long categoryId;

    @Field(type = FieldType.Keyword)
    String categoryName;

    @Field(type = FieldType.Text, index = false)
    String categoryDescription;

    @Field(type = FieldType.Long)
    Long teacherId;

    @Field(type = FieldType.Text, analyzer = "standard")
    String teacherName;

    @Field(type = FieldType.Keyword, index = false)
    String teacherAvatarUrl;

    @Field(type = FieldType.Long)
    Long createdAt;

    @Field(type = FieldType.Keyword)
    Status status;
}