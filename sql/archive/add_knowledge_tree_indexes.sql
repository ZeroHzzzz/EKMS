-- ============================================
-- 知识结构树系统 - 数据库索引优化
-- ============================================

USE knowledge_db;

-- 添加父节点索引，优化树查询
-- 如果索引已存在会报错，可以忽略
ALTER TABLE `knowledge` ADD INDEX `idx_parent_id` (`parent_id`);

-- 添加父节点+排序联合索引，优化同级节点排序查询
ALTER TABLE `knowledge` ADD INDEX `idx_parent_sort` (`parent_id`, `sort_order`);

-- 说明：
-- idx_parent_id: 用于快速查找某个节点的所有子节点
-- idx_parent_sort: 用于快速获取同级节点并按sort_order排序

