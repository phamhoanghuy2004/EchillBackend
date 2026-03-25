package com.echill.mapper;

import com.echill.dto.response.DocumentResponse;
import com.echill.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    // 💥 Lệnh này bảo MapStruct: "Thò tay vào object Lesson, lấy cái ID ra gán vào lessonId"
    @Mapping(source = "lesson.id", target = "lessonId")
    DocumentResponse toDocumentResponse(Document document);

}
