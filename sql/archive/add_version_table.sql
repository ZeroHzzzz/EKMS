-- ============================================
-- 添加知识版本历史表
-- ============================================

USE knowledge_db;

-- 知识版本历史表
CREATE TABLE IF NOT EXISTS `knowledge_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `version` BIGINT NOT NULL COMMENT '版本号',
    `title` VARCHAR(255) COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `summary` VARCHAR(500) COMMENT '摘要',
    `category` VARCHAR(100) COMMENT '分类',
    `keywords` VARCHAR(500) COMMENT '关键词',
    `author` VARCHAR(50) COMMENT '作者',
    `department` VARCHAR(100) COMMENT '部门',
    `file_id` BIGINT COMMENT '关联文件ID',
    `change_description` VARCHAR(500) COMMENT '变更说明',
    `created_by` VARCHAR(50) COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_version` (`knowledge_id`, `version`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识版本历史表';

