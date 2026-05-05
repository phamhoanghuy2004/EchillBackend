package com.echill.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinPackageCheckoutRequest {
    @NotNull(message = "Id gói xu không được để trống")
    Long coinPackageId;
}
