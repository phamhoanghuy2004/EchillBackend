package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tag extends BaseEntity   {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    // Đảm bảo tên Tag không bao giờ bị trùng (Ví dụ: Không thể có 2 tag "TOEIC")
    @Column(nullable = false, unique = true, length = 100)
    String name;
}
