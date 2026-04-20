package com.echill.entity;

import com.echill.entity.enums.TagGroup;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

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
    Long id;

    @Column(nullable = false, unique = true, length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_group", length = 50, nullable = false)
    @Builder.Default
    TagGroup tagGroup = TagGroup.ENGLISH_TOEIC;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        // So sánh an toàn dựa trên tên Tag (ví dụ: "TOEIC")
        return Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
