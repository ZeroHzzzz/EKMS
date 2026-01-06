-- ============================================
-- 知识关联和评论功能相关表
-- ============================================

USE knowledge_db;

-- 知识关联表
CREATE TABLE IF NOT EXISTS `knowledge_relation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `related_knowledge_id` BIGINT NOT NULL COMMENT '关联知识ID',
    `relation_type` VARCHAR(50) DEFAULT 'RELATED' COMMENT '关联类型：RELATED(相关)、REFERENCE(引用)、SIMILAR(相似)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    UNIQUE KEY `uk_knowledge_relation` (`knowledge_id`, `related_knowledge_id`),
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_related_id` (`related_knowledge_id`),
    INDEX `idx_relation_type` (`relation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识关联表';

-- 知识评论表
CREATE TABLE IF NOT EXISTS `knowledge_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT COMMENT '父评论ID（支持回复）',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` VARCHAR(20) DEFAULT 'APPROVED' COMMENT '状态：APPROVED(已通过)、PENDING(待审核)、DELETED(已删除)',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识评论表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    INDEX `idx_comment_id` (`comment_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

