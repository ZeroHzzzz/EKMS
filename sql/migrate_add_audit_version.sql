-- 迁移脚本：为审核表添加版本字段
-- 用于支持"按版本审核"的功能
-- 执行前请备份数据库！

USE knowledge_db;

-- 在audit表中添加version字段（记录审核的是哪个版本）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'audit' AND COLUMN_NAME = 'version');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `audit` ADD COLUMN `version` BIGINT DEFAULT NULL COMMENT ''审核的版本号'' AFTER `knowledge_id`', 'SELECT ''Column version already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'audit' AND INDEX_NAME = 'idx_knowledge_version');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `audit` ADD INDEX `idx_knowledge_version` (`knowledge_id`, `version`)', 'SELECT ''Index idx_knowledge_version already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有审核记录：为没有version的审核记录补充version（设为对应知识的当前版本）
UPDATE `audit` a
INNER JOIN `knowledge` k ON a.knowledge_id = k.id
SET a.version = k.version
WHERE a.version IS NULL;

SELECT '迁移完成！' AS message;

