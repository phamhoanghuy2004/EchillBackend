package com.echill.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudinaryWebhookPayload {
    @JsonProperty("public_id")
    String publicId;

    @JsonProperty("notification_type")
    String notificationType;

    List<EagerItem> eager;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EagerItem {
        @JsonProperty("secure_url")
        String secureUrl;

        String format; // 💥 m3u8, mp4, wbm...
    }
}
