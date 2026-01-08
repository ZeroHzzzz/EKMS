ALTER TABLE `knowledge_version` ADD COLUMN `merge_from_version` BIGINT NULL COMMENT '来源版本（如果是合并产生的版本，记录来源草稿版本）';
