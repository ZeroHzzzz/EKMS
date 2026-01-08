-- 添加 base_version 字段到 knowledge_version 表
-- 用于追踪草稿基于哪个已发布版本创建，以检测并发冲突

ALTER TABLE `knowledge_version` 
ADD COLUMN `base_version` BIGINT NULL COMMENT '基于哪个已发布版本创建（用于检测并发冲突）' 
AFTER `merge_from_version`;

-- 添加索引以加速冲突检测查询
CREATE INDEX `idx_base_version` ON `knowledge_version` (`knowledge_id`, `base_version`);

-- 迁移现有数据：将现有待审核版本的 base_version 设置为当前发布版本
-- 这是一个安全的迁移，不会破坏现有数据
UPDATE `knowledge_version` kv
INNER JOIN `knowledge` k ON kv.knowledge_id = k.id
SET kv.base_version = k.published_version
WHERE kv.status IN ('PENDING', 'DRAFT', 'REJECTED')
  AND kv.base_version IS NULL
  AND k.published_version IS NOT NULL;

-- 对于初始版本（没有发布版本的情况），base_version 保持为 NULL
