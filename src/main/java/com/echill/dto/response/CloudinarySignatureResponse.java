package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CloudinarySignatureResponse {
    String signature;

    long timestamp;

    @JsonProperty("api_key")
    String apiKey;

    @JsonProperty("cloud_name")
    String cloudName;

    String eager;

    @JsonProperty("eager_async")
    boolean eagerAsync;

    String folder;

    String publicId;

    String notificationUrl;

    // Dùng @JsonProperty để lúc convert ra JSON gửi về Frontend, nó tự biến thành api_key, cloud_name giúp code React gọi axios nhàn hơn
}
