package com.pcpart.assembler.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("build_config")
public class BuildConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("config_name")
    private String configName;

    @TableField("cpu_id")
    private String cpuId;

    @TableField("cooler_id")
    private String coolerId;

    @TableField("motherboard_id")
    private String motherboardId;

    @TableField("ram_id")
    private String ramId;

    @TableField("gpu_id")
    private String gpuId;

    @TableField("psu_id")
    private String psuId;

    @TableField("storage_id")
    private String storageId;

    @TableField("case_id")
    private String caseId;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
