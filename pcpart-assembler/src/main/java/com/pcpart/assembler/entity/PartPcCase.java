package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("part_pc_case")
public class PartPcCase {

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

    @TableField("form_factor")
    private String formFactor;

    @TableField("color")
    private String color;

    @TableField("side_panel")
    private String sidePanel;

    @TableField("transparent_panel")
    private Integer transparentPanel;

    @TableField("power_supply")
    private String powerSupply;

    @TableField("max_gpu_length")
    private Integer maxGpuLength;

    @TableField("max_cooler_height")
    private BigDecimal maxCoolerHeight;

    @TableField("bays_35")
    private Integer bays35;

    @TableField("bays_25")
    private Integer bays25;

    @TableField("expansion_slots")
    private Integer expansionSlots;

    @TableField("volume_l")
    private BigDecimal volumeL;

    @TableField("weight_kg")
    private BigDecimal weightKg;

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
