-- 用户收藏关系表
CREATE TABLE IF NOT EXISTS `user_knowledge_collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人',
    `update_by` VARCHAR(100) COMMENT '更新人',
    UNIQUE KEY `uk_user_knowledge` (`user_id`, `knowledge_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏关系表';

