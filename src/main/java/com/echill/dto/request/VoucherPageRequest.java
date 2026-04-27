package com.echill.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VoucherPageRequest extends BasePageRequest {

    public VoucherPageRequest() {
        this.setSortBy("createdAt");
        this.setSortDir("desc");
    }

    @Override
    @JsonIgnore
    protected List<String> getAllowedSortColumns() {
        return List.of("createdAt", "updatedAt", "endDate", "startDate", "discountValue");
    }
}