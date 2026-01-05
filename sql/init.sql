-- 创建数据库
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_db;

-- 部门表
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '部门名称',
    `description` VARCHAR(500) COMMENT '部门描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `department_id` BIGINT COMMENT '部门ID（系统管理员为NULL）',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：ADMIN, EDITOR, USER',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50),
    INDEX `idx_department_id` (`department_id`),
    CONSTRAINT `fk_user_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文件信息表
CREATE TABLE IF NOT EXISTS `file_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_type` VARCHAR(20) COMMENT '文件类型：WORD, EXCEL, PDF等',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `file_hash` VARCHAR(64) COMMENT '文件哈希值',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT, PENDING, APPROVED, REJECTED, ARCHIVED',
    `upload_user_id` BIGINT COMMENT '上传用户ID',
    `preview_url` VARCHAR(500) COMMENT '预览URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50),
    INDEX `idx_file_hash` (`file_hash`),
    INDEX `idx_upload_user` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 分片信息表
CREATE TABLE IF NOT EXISTS `chunk_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `upload_id` VARCHAR(64) NOT NULL COMMENT '上传ID',
    `chunk_index` INT NOT NULL COMMENT '分片索引',
    `chunk_hash` VARCHAR(64) COMMENT '分片哈希',
    `chunk_path` VARCHAR(500) COMMENT '分片路径',
    `file_id` BIGINT COMMENT '文件ID',
    INDEX `idx_upload_id` (`upload_id`),
    INDEX `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片信息表';

-- 知识表
CREATE TABLE IF NOT EXISTS `knowledge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `content_text` LONGTEXT COMMENT '文档全文内容（用于全文搜索，从docx/doc/pdf等文件提取）',
    `summary` VARCHAR(500) COMMENT '摘要',
    `category` VARCHAR(100) COMMENT '分类',
    `keywords` VARCHAR(500) COMMENT '关键词',
    `author` VARCHAR(50) COMMENT '作者',
    `department` VARCHAR(100) COMMENT '部门',
    `file_id` BIGINT COMMENT '关联文件ID',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态',
    `click_count` BIGINT DEFAULT 0 COMMENT '点击次数',
    `collect_count` BIGINT DEFAULT 0 COMMENT '收藏次数',
    `version` BIGINT DEFAULT 1 COMMENT '版本号（最新版本）',
    `published_version` BIGINT DEFAULT NULL COMMENT '已发布的版本号（用户查看的版本）',
    `has_draft` TINYINT(1) DEFAULT 0 COMMENT '是否有待审核的草稿版本',
    `current_branch` VARCHAR(50) DEFAULT 'main' COMMENT '当前分支',
    `current_commit_hash` VARCHAR(64) COMMENT '当前Commit Hash',
    `parent_id` BIGINT COMMENT '父节点ID（用于知识树结构）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_click_count` (`click_count`),
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_parent_sort` (`parent_id`, `sort_order`),
    INDEX `idx_published_version` (`published_version`),
    INDEX `idx_has_draft` (`has_draft`),
    FULLTEXT INDEX `ft_content_text` (`content_text`) WITH PARSER ngram COMMENT '全文索引（MySQL ngram分词）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识表';

-- 审核表
CREATE TABLE IF NOT EXISTS `audit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '审核状态：PENDING, APPROVED, REJECTED',
    `submit_user_id` BIGINT COMMENT '提交用户ID',
    `auditor_id` BIGINT COMMENT '审核人ID',
    `comment` TEXT COMMENT '审核意见',
    `validation_errors` TEXT COMMENT '校验错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50),
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核表';

-- 知识版本历史表
CREATE TABLE IF NOT EXISTS `knowledge_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `version` BIGINT NOT NULL COMMENT '版本号',
    `commit_hash` VARCHAR(64) COMMENT 'Commit哈希（类似Git commit hash）',
    `branch` VARCHAR(50) DEFAULT 'main' COMMENT '分支名称（main/draft/review/user分支等）',
    `parent_commit_id` BIGINT COMMENT '父Commit ID（用于形成commit树）',
    `commit_message` VARCHAR(500) COMMENT 'Commit消息（变更说明）',
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
    `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否为已发布版本',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '版本状态：DRAFT(草稿)、PENDING(待审核)、APPROVED(已发布)、REJECTED(已驳回)',
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_version` (`knowledge_id`, `version`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_commit_hash` (`commit_hash`),
    INDEX `idx_branch` (`knowledge_id`, `branch`),
    INDEX `idx_parent_commit` (`parent_commit_id`),
    INDEX `idx_is_published` (`knowledge_id`, `is_published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识版本历史表';

-- 知识分支表
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

-- 文件版本关联表（用于处理同名文件的不同版本）
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

-- 知识关联表
CREATE TABLE IF NOT EXISTS `knowledge_relation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `related_knowledge_id` BIGINT NOT NULL COMMENT '关联知识ID',
    `relation_type` VARCHAR(50) DEFAULT 'RELATED' COMMENT '关联类型：RELATED(相关)、REFERENCE(引用)、SIMILAR(相似)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
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

-- 注意：此脚本仅创建表结构，不插入任何测试数据
-- 如需测试数据，请使用 test_data.sql 脚本
