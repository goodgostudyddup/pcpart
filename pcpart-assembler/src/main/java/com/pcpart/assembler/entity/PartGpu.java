package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("part_gpu")
public class PartGpu {

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

    @TableField("chipset_manufacturer")
    private String chipsetManufacturer;

    @TableField("chipset")
    private String chipset;

    @TableField("core_base_clock")
    private Integer coreBaseClock;

    @TableField("core_boost_clock")
    private Integer coreBoostClock;

    @TableField("memory_gb")
    private Integer memoryGb;

    @TableField("memory_type")
    private String memoryType;

    @TableField("effective_memory_clock")
    private Integer effectiveMemoryClock;

    @TableField("tdp")
    private Integer tdp;

    @TableField("gpu_interface")
    private String gpuInterface;

    @TableField("length_mm")
    private BigDecimal lengthMm;

    @TableField("slot_width")
    private BigDecimal slotWidth;

    @TableField("cooling")
    private String cooling;

    @TableField("frame_sync")
    private String frameSync;

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
