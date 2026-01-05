-- 迁移脚本：添加发布版本相关字段
-- 用于支持"发布版本"和"草稿版本"分离的功能
-- 执行前请备份数据库！

USE knowledge_db;

-- 检查字段是否存在，不存在则添加
-- 在knowledge表中添加published_version字段（记录当前对外发布的版本号）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge' AND COLUMN_NAME = 'published_version');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge` ADD COLUMN `published_version` BIGINT DEFAULT NULL COMMENT ''已发布的版本号（用户查看的版本）'' AFTER `version`', 'SELECT ''Column published_version already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 在knowledge表中添加has_draft字段（标记是否有待审核的草稿）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge' AND COLUMN_NAME = 'has_draft');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge` ADD COLUMN `has_draft` TINYINT(1) DEFAULT 0 COMMENT ''是否有待审核的草稿版本'' AFTER `published_version`', 'SELECT ''Column has_draft already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge' AND INDEX_NAME = 'idx_published_version');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge` ADD INDEX `idx_published_version` (`published_version`)', 'SELECT ''Index idx_published_version already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge' AND INDEX_NAME = 'idx_has_draft');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge` ADD INDEX `idx_has_draft` (`has_draft`)', 'SELECT ''Index idx_has_draft already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始化现有数据：将当前version设为published_version（对于已发布的内容）
UPDATE `knowledge` SET `published_version` = `version` WHERE `status` = 'APPROVED' AND `published_version` IS NULL;
UPDATE `knowledge` SET `has_draft` = 0 WHERE `has_draft` IS NULL;

-- 在knowledge_version表中添加is_published字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge_version' AND COLUMN_NAME = 'is_published');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge_version` ADD COLUMN `is_published` TINYINT(1) DEFAULT 0 COMMENT ''是否为已发布版本'' AFTER `create_time`', 'SELECT ''Column is_published already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 在knowledge_version表中添加status字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'knowledge_db' AND TABLE_NAME = 'knowledge_version' AND COLUMN_NAME = 'status');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `knowledge_version` ADD COLUMN `status` VARCHAR(20) DEFAULT ''DRAFT'' COMMENT ''版本状态：DRAFT(草稿)、PENDING(待审核)、APPROVED(已发布)、REJECTED(已驳回)'' AFTER `is_published`', 'SELECT ''Column status already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 标记已发布的版本
UPDATE `knowledge_version` kv
INNER JOIN `knowledge` k ON kv.knowledge_id = k.id AND kv.version = k.published_version
SET kv.is_published = 1, kv.status = 'APPROVED'
WHERE k.status = 'APPROVED';

-- 将其他版本状态设置为APPROVED（历史版本）
UPDATE `knowledge_version` SET `status` = 'APPROVED' WHERE `status` IS NULL OR `status` = '' OR `status` = 'DRAFT';

SELECT '迁移完成！' AS message;

