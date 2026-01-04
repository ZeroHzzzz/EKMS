-- 数据库迁移脚本：添加 content_text 字段用于文档全文搜索
-- 执行时间：请在应用停止时执行，避免数据不一致

USE knowledge_db;

-- 检查并添加 content_text 字段
-- 如果字段已存在，此语句会报错，可以忽略
ALTER TABLE `knowledge` 
ADD COLUMN `content_text` LONGTEXT COMMENT '文档全文内容（用于全文搜索，从docx/doc/pdf等文件提取）' 
AFTER `content`;

-- 添加全文索引（使用 ngram 分词器支持中文）
-- 注意：MySQL 5.7+ 支持 ngram 分词器
-- 如果索引已存在，此语句会报错，可以忽略
ALTER TABLE `knowledge` 
ADD FULLTEXT INDEX `ft_content_text` (`content_text`) WITH PARSER ngram;

-- 查看表结构确认字段已添加
DESCRIBE `knowledge`;

