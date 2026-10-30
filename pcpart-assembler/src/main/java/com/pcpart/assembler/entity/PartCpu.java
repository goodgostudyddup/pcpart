package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("part_cpu")
public class PartCpu {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("opendb_id")
    private String opendbId;

    @TableField("name")
    private String name;

    @TableField("manufacturer")
    private String manufacturer;

    @TableField("series")
    private String series;

    @TableField("variant")
    private String variant;

    @TableField("socket")
    private String socket;

    @TableField("microarchitecture")
    private String microarchitecture;

    @TableField("core_family")
    private String coreFamily;

    @TableField("cores_total")
    private Integer coresTotal;

    @TableField("cores_performance")
    private Integer coresPerformance;

    @TableField("cores_efficiency")
    private Integer coresEfficiency;

    @TableField("threads")
    private Integer threads;

    @TableField("base_clock")
    private BigDecimal baseClock;

    @TableField("boost_clock")
    private BigDecimal boostClock;

    @TableField("l2_cache")
    private BigDecimal l2Cache;

    @TableField("l3_cache")
    private BigDecimal l3Cache;

    @TableField("tdp")
    private Integer tdp;

    @TableField("integrated_graphics")
    private String integratedGraphics;

    @TableField("memory_max")
    private Integer memoryMax;

    @TableField("release_year")
    private Integer releaseYear;

    @TableField("price_usd")
    private java.math.BigDecimal priceUsd;

    @TableField("price_cny")
    private java.math.BigDecimal priceCny;

    @TableField("price_updated_at")
    private java.time.LocalDateTime priceUpdatedAt;

    @TableField("raw_json")
    private String rawJson;
}
