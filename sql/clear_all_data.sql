-- ============================================
-- 清空数据库所有数据脚本
-- ============================================
-- 注意：此脚本会删除所有业务数据，请谨慎使用！
-- 使用前请备份数据库！
-- 执行后所有表的数据将被清空，但表结构会保留

USE knowledge_db;

-- 关闭外键检查（如果有外键约束）
SET FOREIGN_KEY_CHECKS = 0;

-- 按照依赖关系顺序清空表数据
-- 1. 清空审核表（依赖knowledge表）
DELETE FROM `audit`;

-- 2. 清空知识表（依赖file_info和user表）
DELETE FROM `knowledge`;

-- 3. 清空分片信息表（依赖file_info表）
DELETE FROM `chunk_info`;

-- 4. 清空文件信息表（依赖user表）
DELETE FROM `file_info`;

-- 5. 清空用户表
DELETE FROM `user`;

-- 重新开启外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 重置自增ID（让新插入的数据从1开始）
ALTER TABLE `user` AUTO_INCREMENT = 1;
ALTER TABLE `file_info` AUTO_INCREMENT = 1;
ALTER TABLE `chunk_info` AUTO_INCREMENT = 1;
ALTER TABLE `knowledge` AUTO_INCREMENT = 1;
ALTER TABLE `audit` AUTO_INCREMENT = 1;

-- 验证清空结果
SELECT 
    (SELECT COUNT(*) FROM `user`) AS user_count,
    (SELECT COUNT(*) FROM `file_info`) AS file_info_count,
    (SELECT COUNT(*) FROM `chunk_info`) AS chunk_info_count,
    (SELECT COUNT(*) FROM `knowledge`) AS knowledge_count,
    (SELECT COUNT(*) FROM `audit`) AS audit_count;

-- 说明：
-- 执行此脚本后，所有业务数据将被清空
-- 表结构保持不变，可以重新插入数据
-- 
-- 你可以通过以下方式使用此脚本：
-- 1. 在MySQL客户端中执行：source sql/clear_all_data.sql
-- 2. 或者在命令行执行：mysql -u用户名 -p密码 knowledge_db < sql/clear_all_data.sql
-- 3. 或者在Docker中执行：docker exec -i mysql容器名 mysql -uroot -p密码 knowledge_db < sql/clear_all_data.sql

