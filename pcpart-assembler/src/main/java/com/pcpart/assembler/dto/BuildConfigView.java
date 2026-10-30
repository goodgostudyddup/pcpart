package com.pcpart.assembler.dto;

import com.pcpart.assembler.entity.BuildConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 配置单展示对象：在原 build_config 字段之外，附带各配件 opendb_id 对应的产品名称。
 * 列表页与详情页直接读 *Name 字段，避免前端只拿到一串编号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BuildConfigView extends BuildConfig {

    /** CPU 名称，例如 Intel Core i5 13500 */
    private String cpuName;

    /** 散热器名称 */
    private String coolerName;

    /** 主板名称 */
    private String motherboardName;

    /** 内存名称 */
    private String ramName;

    /** 显卡名称，例如 PNY GeForce RTX 4060 Ti 8GB VERTO Dual Fan DLSS 3 GDDR6 */
    private String gpuName;

    /** 电源名称 */
    private String psuName;

    /** 硬盘名称 */
    private String storageName;

    /** 机箱名称 */
    private String caseName;
}
