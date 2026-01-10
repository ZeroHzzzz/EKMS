ALTER TABLE knowledge ADD COLUMN is_private TINYINT(1) DEFAULT 0 COMMENT '是否私有文档';

CREATE TABLE IF NOT EXISTS `knowledge_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `knowledge_id` bigint(20) NOT NULL COMMENT '知识ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `permission_type` varchar(20) NOT NULL COMMENT '权限类型: VIEW, EDIT',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_user` (`knowledge_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档权限表';
