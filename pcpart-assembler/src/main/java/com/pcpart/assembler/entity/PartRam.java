package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("part_ram")
public class PartRam {

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

    @TableField("ram_type")
    private String ramType;

    @TableField("speed_mt")
    private Integer speedMt;

    @TableField("capacity_gb")
    private Integer capacityGb;

    @TableField("modules_qty")
    private Integer modulesQty;

    @TableField("module_gb")
    private Integer moduleGb;

    @TableField("form_factor")
    private String formFactor;

    @TableField("cas_latency")
    private Integer casLatency;

    @TableField("voltage")
    private BigDecimal voltage;

    @TableField("color")
    private String color;

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
