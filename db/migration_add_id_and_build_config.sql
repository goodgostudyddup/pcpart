-- pcpart 配置单项目数据库迁移脚本
-- 执行内容：
-- 1. 给 9 张旧表（无主键、有重名行）各加一个自增 id 主键（FIRST 列，不破坏原有列与数据）
-- 2. 新建配置单表 build_config（单表存各配件名 + 总价）
-- 账号：root / root，库名：pcpart，主机：127.0.0.1:3306
-- 本脚本可重复执行（使用存储过程判断列/表是否存在）

DELIMITER $$

DROP PROCEDURE IF EXISTS pcpart_add_id_if_missing $$
CREATE PROCEDURE pcpart_add_id_if_missing(IN p_table VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'pcpart' AND TABLE_NAME = p_table AND COLUMN_NAME = 'id'
    ) THEN
        SET @sql_text = CONCAT('ALTER TABLE `pcpart`.`', p_table, '` ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST');
        PREPARE stmt FROM @sql_text;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL pcpart_add_id_if_missing('case_info');
CALL pcpart_add_id_if_missing('cpu');
CALL pcpart_add_id_if_missing('internal_hard_drive');
CALL pcpart_add_id_if_missing('laptops');
CALL pcpart_add_id_if_missing('memory');
CALL pcpart_add_id_if_missing('monitor');
CALL pcpart_add_id_if_missing('motherboard');
CALL pcpart_add_id_if_missing('power_supply');
CALL pcpart_add_id_if_missing('video_card');

DROP PROCEDURE IF EXISTS pcpart_add_id_if_missing;

CREATE TABLE IF NOT EXISTS `pcpart`.`build_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置单主键',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置单名称',
    `cpu_name` VARCHAR(255) NULL DEFAULT NULL COMMENT 'CPU型号名称',
    `motherboard_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '主板型号名称',
    `memory_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '内存型号名称',
    `video_card_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '显卡型号名称',
    `power_supply_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '电源型号名称',
    `hard_drive_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '硬盘型号名称',
    `case_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '机箱型号名称',
    `monitor_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '显示器型号名称',
    `laptop_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '笔记本型号（可选，整机方案时使用）',
    `total_price` DECIMAL(12, 2) NULL DEFAULT NULL COMMENT '整单总价(欧元)',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '电脑组装配置单表：单表存各配件名称+总价';
