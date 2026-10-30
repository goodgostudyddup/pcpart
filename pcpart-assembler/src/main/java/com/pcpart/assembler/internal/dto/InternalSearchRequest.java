package com.pcpart.assembler.internal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class InternalSearchRequest {

    private long current = 1;
    private long size = 20;
    private String keyword;
    private Map<String, Object> filters = new HashMap<>();
    private BigDecimal priceCnyMin;
    private BigDecimal priceCnyMax;
    private BigDecimal priceUsdMin;
    private BigDecimal priceUsdMax;
    private Boolean requirePriced;
    private String sortBy;
    private String sortOrder;
}
