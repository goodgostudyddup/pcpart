-- 给 8 张 part_* 表追加价格列（BuildCores 美元最低价 + 人民币换算价）
-- 可重复执行
SET NAMES utf8mb4;

ALTER TABLE `part_cpu`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_cpu_cooler`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_gpu`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_motherboard`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_pc_case`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_psu`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_ram`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
ALTER TABLE `part_storage`
  ADD COLUMN IF NOT EXISTS `price_usd` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price',
  ADD COLUMN IF NOT EXISTS `price_cny` DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price',
  ADD COLUMN IF NOT EXISTS `price_updated_at` DATETIME NULL DEFAULT NULL COMMENT 'price updated at';
