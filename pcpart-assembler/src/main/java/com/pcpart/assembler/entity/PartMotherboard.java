package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("part_motherboard")
public class PartMotherboard {

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

    @TableField("chipset")
    private String chipset;

    @TableField("form_factor")
    private String formFactor;

    @TableField("memory_max")
    private Integer memoryMax;

    @TableField("memory_type")
    private String memoryType;

    @TableField("memory_slots")
    private Integer memorySlots;

    @TableField("wireless")
    private String wireless;

    @TableField("ecc_support")
    private Integer eccSupport;

    @TableField("raid_support")
    private Integer raidSupport;

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
