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
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Conversation extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(length = 200) String name; // Nullable vì chat 1-1 thường không có tên

    @Column(name = "last_message_at")
    java.time.Instant lastMessageAt;
}
