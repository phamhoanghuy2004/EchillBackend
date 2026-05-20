package com.echill.entity;

import com.echill.entity.enums.TagGroup;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_parent", columnList = "parent_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tag extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, unique = true, length = 100)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Tag parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    List<Tag> subTags = new ArrayList<>();

    @Column(name = "min_level", nullable = false)
    @Builder.Default
    Integer minLevel = 1;

    @Column(name = "max_level", nullable = false)
    @Builder.Default
    Integer maxLevel = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_group", length = 50, nullable = false)
    @Builder.Default
    TagGroup tagGroup = TagGroup.ENGLISH_TOEIC;

    public void addSubTag(Tag child) {
        subTags.add(child);
        child.setParent(this);
    }

    public void removeSubTag(Tag child) {
        subTags.remove(child);
        child.setParent(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}