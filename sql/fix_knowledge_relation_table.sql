-- ============================================
-- 修复 knowledge_relation 表，添加缺失的 update_time 和 update_by 列
-- ============================================

USE knowledge_db;

-- 添加 update_time 列（如果已存在会报错，可以忽略）
-- 执行前请先检查列是否存在，避免重复执行报错
ALTER TABLE `knowledge_relation` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;

-- 添加 update_by 列（如果已存在会报错，可以忽略）
ALTER TABLE `knowledge_relation` 
ADD COLUMN `update_by` VARCHAR(50) COMMENT '更新人' AFTER `create_by`;

