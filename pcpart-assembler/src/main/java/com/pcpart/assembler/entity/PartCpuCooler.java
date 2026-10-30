package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("part_cpu_cooler")
public class PartCpuCooler {

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

    @TableField("water_cooled")
    private Integer waterCooled;

    @TableField("radiator_size")
    private Integer radiatorSize;

    @TableField("fanless")
    private Integer fanless;

    @TableField("fan_size")
    private Integer fanSize;

    @TableField("fan_quantity")
    private Integer fanQuantity;

    @TableField("min_fan_rpm")
    private Integer minFanRpm;

    @TableField("max_fan_rpm")
    private Integer maxFanRpm;

    @TableField("min_noise")
    private BigDecimal minNoise;

    @TableField("max_noise")
    private BigDecimal maxNoise;

    @TableField("height")
    private BigDecimal height;

    @TableField("cpu_sockets")
    private String cpuSockets;

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
