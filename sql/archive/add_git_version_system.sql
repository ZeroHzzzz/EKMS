-- ============================================
-- 基于Git理念的知识版本管理系统
-- ============================================

USE knowledge_db;

-- 扩展知识版本表，添加Git相关字段
-- 注意：如果字段已存在会报错，可以忽略
ALTER TABLE `knowledge_version` 
ADD COLUMN `commit_hash` VARCHAR(64) COMMENT 'Commit哈希（类似Git commit hash）' AFTER `version`,
ADD COLUMN `branch` VARCHAR(50) DEFAULT 'main' COMMENT '分支名称（main/draft/review/user分支等）' AFTER `commit_hash`,
ADD COLUMN `parent_commit_id` BIGINT COMMENT '父Commit ID（用于形成commit树）' AFTER `branch`,
ADD COLUMN `commit_message` VARCHAR(500) COMMENT 'Commit消息（变更说明）' AFTER `parent_commit_id`;

-- 添加索引
CREATE INDEX `idx_commit_hash` ON `knowledge_version` (`commit_hash`);
CREATE INDEX `idx_branch` ON `knowledge_version` (`knowledge_id`, `branch`);
CREATE INDEX `idx_parent_commit` ON `knowledge_version` (`parent_commit_id`);

-- 添加知识分支表
CREATE TABLE IF NOT EXISTS `knowledge_branch` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `branch_name` VARCHAR(50) NOT NULL COMMENT '分支名称',
    `current_commit_id` BIGINT COMMENT '当前分支指向的commit ID',
    `description` VARCHAR(500) COMMENT '分支描述',
    `created_by` VARCHAR(50) COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_knowledge_branch` (`knowledge_id`, `branch_name`),
    INDEX `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分支表';

-- 更新现有的knowledge表，添加当前分支和当前commit字段
ALTER TABLE `knowledge`
ADD COLUMN `current_branch` VARCHAR(50) DEFAULT 'main' COMMENT '当前分支' AFTER `version`,
ADD COLUMN `current_commit_hash` VARCHAR(64) COMMENT '当前Commit Hash' AFTER `current_branch`;

-- 添加文件版本关联表（用于处理同名文件的不同版本）
CREATE TABLE IF NOT EXISTS `file_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `commit_hash` VARCHAR(64) NOT NULL COMMENT '关联的Commit Hash',
    `knowledge_id` BIGINT NOT NULL COMMENT '关联的知识ID',
    `version_number` INT NOT NULL COMMENT '文件版本号（同名文件的第几个版本）',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否激活（当前使用的版本）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_commit_hash` (`commit_hash`),
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_file_name` (`file_name`, `knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件版本关联表';
