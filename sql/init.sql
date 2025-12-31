-- 创建数据库
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `department` VARCHAR(100) COMMENT '部门',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：ADMIN, AUDITOR, USER',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50)
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
    `summary` VARCHAR(500) COMMENT '摘要',
    `category` VARCHAR(100) COMMENT '分类',
    `keywords` VARCHAR(500) COMMENT '关键词',
    `author` VARCHAR(50) COMMENT '作者',
    `department` VARCHAR(100) COMMENT '部门',
    `file_id` BIGINT COMMENT '关联文件ID',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态',
    `click_count` BIGINT DEFAULT 0 COMMENT '点击次数',
    `collect_count` BIGINT DEFAULT 0 COMMENT '收藏次数',
    `version` BIGINT DEFAULT 1 COMMENT '版本号',
    `parent_id` BIGINT COMMENT '父节点ID（用于知识树结构）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` VARCHAR(50),
    `update_by` VARCHAR(50),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_click_count` (`click_count`),
    INDEX `idx_file_id` (`file_id`)
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

-- 插入测试数据（基础测试账号，详细测试数据请使用 test_data.sql）
INSERT INTO `user` (`username`, `password`, `real_name`, `email`, `department`, `role`) VALUES
('admin', 'admin123', '管理员', 'admin@example.com', 'IT部门', 'ADMIN'),
('editor1', 'editor123', '知识管理员1', 'editor1@example.com', '业务部', 'EDITOR'),
('user1', 'user123', '用户1', 'user1@example.com', '业务部', 'USER');

