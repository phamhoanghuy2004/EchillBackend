package com.echill.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoinPackagePageRequest extends BasePageRequest {

    public CoinPackagePageRequest() {
        this.setSortBy("price");
        this.setSortDir("asc");
    }

    @Override
    @JsonIgnore
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "price", "coinAmount", "name");
    }
}
