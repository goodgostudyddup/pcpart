-- BuildCore open-db 新结构迁移：删除旧表，新建 8 张富参数表 + 重建配置单
-- 关键列 + raw_json 列；opendb_id 全局唯一。
-- 原库备份：执行前请自行 mysqldump，本脚本会 DROP 旧表。
-- 可重复执行（DROP IF EXISTS + CREATE TABLE）。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `build_config`;
DROP TABLE IF EXISTS `case_info`;
DROP TABLE IF EXISTS `cpu`;
DROP TABLE IF EXISTS `internal_hard_drive`;
DROP TABLE IF EXISTS `laptops`;
DROP TABLE IF EXISTS `memory`;
DROP TABLE IF EXISTS `monitor`;
DROP TABLE IF EXISTS `motherboard`;
DROP TABLE IF EXISTS `power_supply`;
DROP TABLE IF EXISTS `video_card`;
DROP TABLE IF EXISTS `part_cpu`;
DROP TABLE IF EXISTS `part_cpu_cooler`;
DROP TABLE IF EXISTS `part_gpu`;
DROP TABLE IF EXISTS `part_motherboard`;
DROP TABLE IF EXISTS `part_pc_case`;
DROP TABLE IF EXISTS `part_psu`;
DROP TABLE IF EXISTS `part_ram`;
DROP TABLE IF EXISTS `part_storage`;

-- CPU
CREATE TABLE `part_cpu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `socket` VARCHAR(50) NULL DEFAULT NULL COMMENT '插槽',
  `microarchitecture` VARCHAR(100) NULL DEFAULT NULL COMMENT '微架构',
  `core_family` VARCHAR(100) NULL DEFAULT NULL COMMENT '核心家族',
  `cores_total` INT NULL DEFAULT NULL COMMENT '总核心数',
  `cores_performance` INT NULL DEFAULT NULL COMMENT '性能核数',
  `cores_efficiency` INT NULL DEFAULT NULL COMMENT '能效核数',
  `threads` INT NULL DEFAULT NULL COMMENT '线程数',
  `base_clock` DECIMAL(6,2) NULL DEFAULT NULL COMMENT '基础频率(GHz)',
  `boost_clock` DECIMAL(6,2) NULL DEFAULT NULL COMMENT '加速频率(GHz)',
  `l2_cache` DECIMAL(10,2) NULL DEFAULT NULL COMMENT 'L2缓存(MB)',
  `l3_cache` DECIMAL(10,2) NULL DEFAULT NULL COMMENT 'L3缓存(MB)',
  `tdp` INT NULL DEFAULT NULL COMMENT '功耗(W)',
  `integrated_graphics` VARCHAR(100) NULL DEFAULT NULL COMMENT '核显型号',
  `memory_max` INT NULL DEFAULT NULL COMMENT '最大内存(GB)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cpu_opendb` (`opendb_id`),
  KEY `idx_cpu_name` (`name`),
  KEY `idx_cpu_socket` (`socket`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'CPU部件表(open-db)';

-- CPU散热器
CREATE TABLE `part_cpu_cooler` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `water_cooled` TINYINT(1) NULL DEFAULT NULL COMMENT '是否水冷',
  `radiator_size` INT NULL DEFAULT NULL COMMENT '冷排尺寸(mm)',
  `fanless` TINYINT(1) NULL DEFAULT NULL COMMENT '是否无风扇',
  `fan_size` INT NULL DEFAULT NULL COMMENT '风扇尺寸(mm)',
  `fan_quantity` INT NULL DEFAULT NULL COMMENT '风扇数量',
  `min_fan_rpm` INT NULL DEFAULT NULL COMMENT '最低转速',
  `max_fan_rpm` INT NULL DEFAULT NULL COMMENT '最高转速',
  `min_noise` DECIMAL(6,2) NULL DEFAULT NULL COMMENT '最低噪音(dBA)',
  `max_noise` DECIMAL(6,2) NULL DEFAULT NULL COMMENT '最高噪音(dBA)',
  `height` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '高度(mm)',
  `cpu_sockets` VARCHAR(500) NULL DEFAULT NULL COMMENT '支持插槽(逗号分隔)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cooler_opendb` (`opendb_id`),
  KEY `idx_cooler_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'CPU散热器部件表(open-db)';

-- GPU显卡
CREATE TABLE `part_gpu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `chipset_manufacturer` VARCHAR(50) NULL DEFAULT NULL COMMENT '芯片厂商',
  `chipset` VARCHAR(100) NULL DEFAULT NULL COMMENT '芯片组',
  `core_base_clock` INT NULL DEFAULT NULL COMMENT '核心基础频率(MHz)',
  `core_boost_clock` INT NULL DEFAULT NULL COMMENT '核心加速频率(MHz)',
  `memory_gb` INT NULL DEFAULT NULL COMMENT '显存(GB)',
  `memory_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '显存类型',
  `effective_memory_clock` INT NULL DEFAULT NULL COMMENT '等效显存频率(MHz)',
  `tdp` INT NULL DEFAULT NULL COMMENT '功耗(W)',
  `gpu_interface` VARCHAR(50) NULL DEFAULT NULL COMMENT '接口',
  `length_mm` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '长度(mm)',
  `slot_width` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '槽位宽度',
  `cooling` VARCHAR(100) NULL DEFAULT NULL COMMENT '散热方式',
  `frame_sync` VARCHAR(50) NULL DEFAULT NULL COMMENT '画面同步技术',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gpu_opendb` (`opendb_id`),
  KEY `idx_gpu_name` (`name`),
  KEY `idx_gpu_chipset` (`chipset`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '显卡部件表(open-db)';

-- 主板
CREATE TABLE `part_motherboard` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `socket` VARCHAR(50) NULL DEFAULT NULL COMMENT '插槽',
  `chipset` VARCHAR(100) NULL DEFAULT NULL COMMENT '芯片组',
  `form_factor` VARCHAR(50) NULL DEFAULT NULL COMMENT '板型',
  `memory_max` INT NULL DEFAULT NULL COMMENT '最大内存(GB)',
  `memory_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '内存类型',
  `memory_slots` INT NULL DEFAULT NULL COMMENT '内存插槽数',
  `wireless` VARCHAR(100) NULL DEFAULT NULL COMMENT '无线网络',
  `ecc_support` TINYINT(1) NULL DEFAULT NULL COMMENT 'ECC支持',
  `raid_support` TINYINT(1) NULL DEFAULT NULL COMMENT 'RAID支持',
  `color` VARCHAR(255) NULL DEFAULT NULL COMMENT '颜色(逗号分隔)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mb_opendb` (`opendb_id`),
  KEY `idx_mb_name` (`name`),
  KEY `idx_mb_socket` (`socket`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主板部件表(open-db)';

-- 机箱
CREATE TABLE `part_pc_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `form_factor` VARCHAR(50) NULL DEFAULT NULL COMMENT '机箱版型',
  `color` VARCHAR(255) NULL DEFAULT NULL COMMENT '颜色(逗号分隔)',
  `side_panel` VARCHAR(100) NULL DEFAULT NULL COMMENT '侧板类型',
  `transparent_panel` TINYINT(1) NULL DEFAULT NULL COMMENT '是否透侧',
  `power_supply` VARCHAR(100) NULL DEFAULT NULL COMMENT '附带电源',
  `max_gpu_length` INT NULL DEFAULT NULL COMMENT '最大显卡长度(mm)',
  `max_cooler_height` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '最大散热器高度(mm)',
  `bays_35` INT NULL DEFAULT NULL COMMENT '3.5英寸仓位数',
  `bays_25` INT NULL DEFAULT NULL COMMENT '2.5英寸仓位数',
  `expansion_slots` INT NULL DEFAULT NULL COMMENT '扩展槽数',
  `volume_l` DECIMAL(10,3) NULL DEFAULT NULL COMMENT '体积(升)',
  `weight_kg` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '重量(kg)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_opendb` (`opendb_id`),
  KEY `idx_case_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '机箱部件表(open-db)';

-- 电源
CREATE TABLE `part_psu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `wattage` INT NULL DEFAULT NULL COMMENT '额定功率(W)',
  `form_factor` VARCHAR(50) NULL DEFAULT NULL COMMENT '规格',
  `efficiency` VARCHAR(50) NULL DEFAULT NULL COMMENT '能效等级',
  `modular` VARCHAR(50) NULL DEFAULT NULL COMMENT '模组类型',
  `length_mm` INT NULL DEFAULT NULL COMMENT '长度(mm)',
  `fanless` TINYINT(1) NULL DEFAULT NULL COMMENT '是否无风扇',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_psu_opendb` (`opendb_id`),
  KEY `idx_psu_name` (`name`),
  KEY `idx_psu_wattage` (`wattage`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '电源部件表(open-db)';

-- 内存
CREATE TABLE `part_ram` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `ram_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '内存类型',
  `speed_mt` INT NULL DEFAULT NULL COMMENT '速率(MT/s)',
  `capacity_gb` INT NULL DEFAULT NULL COMMENT '总容量(GB)',
  `modules_qty` INT NULL DEFAULT NULL COMMENT '模组数量',
  `module_gb` INT NULL DEFAULT NULL COMMENT '单条容量(GB)',
  `form_factor` VARCHAR(50) NULL DEFAULT NULL COMMENT '规格',
  `cas_latency` INT NULL DEFAULT NULL COMMENT 'CAS时序',
  `voltage` DECIMAL(5,3) NULL DEFAULT NULL COMMENT '电压(V)',
  `color` VARCHAR(255) NULL DEFAULT NULL COMMENT '颜色(逗号分隔)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ram_opendb` (`opendb_id`),
  KEY `idx_ram_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内存部件表(open-db)';

-- 硬盘
CREATE TABLE `part_storage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opendb_id` CHAR(36) NOT NULL COMMENT 'open-db唯一ID',
  `name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `manufacturer` VARCHAR(100) NULL DEFAULT NULL COMMENT '厂商',
  `series` VARCHAR(100) NULL DEFAULT NULL COMMENT '系列',
  `variant` VARCHAR(100) NULL DEFAULT NULL COMMENT '型号变体',
  `storage_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '类型(SSD/HDD)',
  `capacity_gb` INT NULL DEFAULT NULL COMMENT '容量(GB)',
  `form_factor` VARCHAR(50) NULL DEFAULT NULL COMMENT '规格',
  `hard_interface` VARCHAR(100) NULL DEFAULT NULL COMMENT '接口',
  `nvme` TINYINT(1) NULL DEFAULT NULL COMMENT '是否NVMe',
  `cache_mb` INT NULL DEFAULT NULL COMMENT '缓存(MB)',
  `release_year` INT NULL DEFAULT NULL COMMENT '发布年份',
  `raw_json` JSON NOT NULL COMMENT '原始JSON全文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storage_opendb` (`opendb_id`),
  KEY `idx_storage_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '硬盘部件表(open-db)';

-- 配置单（新结构：引用各部件opendb_id，不再存价格，价格后续爬回填）
CREATE TABLE `build_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置单主键',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置单名称',
  `cpu_id` CHAR(36) NULL DEFAULT NULL COMMENT 'CPU opendb_id',
  `cooler_id` CHAR(36) NULL DEFAULT NULL COMMENT '散热器 opendb_id',
  `motherboard_id` CHAR(36) NULL DEFAULT NULL COMMENT '主板 opendb_id',
  `ram_id` CHAR(36) NULL DEFAULT NULL COMMENT '内存 opendb_id',
  `gpu_id` CHAR(36) NULL DEFAULT NULL COMMENT '显卡 opendb_id',
  `psu_id` CHAR(36) NULL DEFAULT NULL COMMENT '电源 opendb_id',
  `storage_id` CHAR(36) NULL DEFAULT NULL COMMENT '硬盘 opendb_id',
  `case_id` CHAR(36) NULL DEFAULT NULL COMMENT '机箱 opendb_id',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '电脑组装配置单表(open-db引用)';

SET FOREIGN_KEY_CHECKS = 1;
